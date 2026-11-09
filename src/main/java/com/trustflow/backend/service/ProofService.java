package com.trustflow.backend.service;

import com.trustflow.backend.model.Document;
import com.trustflow.backend.model.OnchainProof;
import com.trustflow.backend.repo.DocumentRepository;
import com.trustflow.backend.repo.OnchainProofRepository;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class ProofService {

    private static final Logger logger = LoggerFactory.getLogger(ProofService.class);

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final TransactionManager txManager;
    private final TransactionReceiptProcessor receiptProcessor;
    private final BigInteger gasLimit = BigInteger.valueOf(300_000);
    private final DocumentRepository documentRepository;

    public ProofService(
            @Value("${blockchain.rpc-url}") String rpcUrl,
            @Value("${blockchain.private-key}") String privateKey,
            @Value("${blockchain.contract-address}") String contractAddress,
            DocumentRepository documentRepository) {
        if (rpcUrl == null || rpcUrl.isBlank() || privateKey == null || privateKey.isBlank()
                || contractAddress == null || contractAddress.isBlank()) {
            logger.warn("Blockchain configuration incomplete; proof submission is disabled");
            this.web3j = null;
            this.credentials = null;
            this.contractAddress = contractAddress;
            this.txManager = null;
            this.receiptProcessor = null;
        } else {
            this.web3j = Web3j.build(new HttpService(rpcUrl));
            this.credentials = Credentials.create(privateKey);
            this.contractAddress = contractAddress;
            this.txManager = new RawTransactionManager(web3j, credentials);



            this.receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 3000, 100);
        }
        this.documentRepository = documentRepository;
    }

    private String keccak256Hex(String text) {
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        Keccak.Digest256 digest256 = new Keccak.Digest256();
        byte[] hash = digest256.digest(input);
        return Numeric.toHexStringNoPrefix(hash);
    }

    /**

     */
    public CompletableFuture<String> submitProofAsync(String text, Long generationRecordId,
                                                      OnchainProofRepository onchainRepo) {
        if (web3j == null || txManager == null) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            String hashNoPrefix = keccak256Hex(text);
            String hex32 = "0x" + hashNoPrefix;

            Function function = new Function(
                    "storeProof",
                    Collections.singletonList(new Bytes32(Numeric.hexStringToByteArray(hex32))),
                    Collections.emptyList());
            String encoded = FunctionEncoder.encode(function);

            String txHash;
            synchronized (this) {


                BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice()
                        .multiply(BigInteger.valueOf(200)) // 200%
                        .divide(BigInteger.valueOf(100));

                EthSendTransaction ethSend = txManager.sendTransaction(
                        gasPrice, gasLimit, contractAddress, encoded, BigInteger.ZERO);
                if (ethSend.hasError()) throw new RuntimeException("Blockchain Error: " + ethSend.getError().getMessage());
                txHash = ethSend.getTransactionHash();
            }

            OnchainProof proof = new OnchainProof();
            proof.setGenerationRecordId(generationRecordId);
            proof.setContentHash("0x" + hashNoPrefix);
            proof.setTxHash(txHash);
            proof.setContractAddress(contractAddress);
            proof.setStatus("pending");
            proof.setUpdatedAt(java.time.Instant.now());
            onchainRepo.save(proof);


            CompletableFuture.runAsync(() -> {
                try {
                    TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);
                    proof.setStatus(receipt.isStatusOK() ? "succeeded" : "failed");
                    proof.setUpdatedAt(java.time.Instant.now());
                    onchainRepo.save(proof);
                } catch (Exception e) {
                    proof.setStatus("failed");
                    onchainRepo.save(proof);
                }
            });

            return CompletableFuture.completedFuture(txHash);
        } catch (Exception ex) {
            logger.error("Error submitting chat proof: {}", ex.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**

     */
    public CompletableFuture<String> submitDocumentProofAsync(String fileHash, Long documentId,
                                                              OnchainProofRepository onchainRepo) {
        if (web3j == null || txManager == null) {
            logger.error("ProofService not initialized");
            updateDocumentStatus(documentId, "failed", null);
            return CompletableFuture.completedFuture(null);
        }

        try {
            String hashNoPrefix = fileHash.startsWith("0x") ? fileHash.substring(2) : fileHash;
            if (hashNoPrefix.length() != 64) throw new IllegalArgumentException("Invalid fileHash length");
            String hex32 = "0x" + hashNoPrefix;

            Function function = new Function(
                    "storeProof",
                    Collections.singletonList(new Bytes32(Numeric.hexStringToByteArray(hex32))),
                    Collections.emptyList());
            String encoded = FunctionEncoder.encode(function);

            String txHash;
            synchronized (this) {


                BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice()
                        .multiply(BigInteger.valueOf(200)) // 200%
                        .divide(BigInteger.valueOf(100));

                EthSendTransaction ethSend = txManager.sendTransaction(
                        gasPrice, gasLimit, contractAddress, encoded, BigInteger.ZERO);

                if (ethSend.hasError()) {
                    throw new RuntimeException("Blockchain Node Error: " + ethSend.getError().getMessage());
                }
                txHash = ethSend.getTransactionHash();
            }

            logger.info("Document transaction sent (FAST MODE), txHash: {}", txHash);


            OnchainProof proof = new OnchainProof();
            proof.setContentHash(hex32);
            proof.setTxHash(txHash);
            proof.setContractAddress(contractAddress);
            proof.setStatus("pending");
            proof.setUpdatedAt(java.time.Instant.now());
            onchainRepo.save(proof);


            updateDocumentStatus(documentId, "processing", txHash);


            CompletableFuture.runAsync(() -> {
                try {

                    TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);
                    boolean success = receipt.isStatusOK();

                    proof.setStatus(success ? "succeeded" : "failed");
                    proof.setUpdatedAt(java.time.Instant.now());
                    onchainRepo.save(proof);

                    updateDocumentStatus(documentId, success ? "indexed" : "failed", txHash);

                    if (success) {
                        logger.info("✅ Document proof confirmed on chain! DocId: {}", documentId);
                    } else {
                        logger.error("❌ Transaction reverted on chain. DocId: {}", documentId);
                    }

                } catch (Exception e) {
                    logger.error("Error waiting for document receipt (Timeout or Network)", e);
                    proof.setStatus("failed");
                    onchainRepo.save(proof);

                    updateDocumentStatus(documentId, "failed", txHash);
                }
            });

            return CompletableFuture.completedFuture(txHash);

        } catch (Exception ex) {
            logger.error("Error submitting document proof (Send Stage): {}", ex.getMessage());

            try {
                OnchainProof proof = new OnchainProof();
                String hashNoPrefix = fileHash.startsWith("0x") ? fileHash.substring(2) : fileHash;
                proof.setContentHash("0x" + hashNoPrefix);
                proof.setStatus("failed");
                proof.setUpdatedAt(java.time.Instant.now());
                onchainRepo.save(proof);
            } catch (Exception e) { /* ignore */ }

            updateDocumentStatus(documentId, "failed", "Error: " + ex.getMessage());
            throw new RuntimeException("Blockchain submission failed", ex);
        }
    }

    private void updateDocumentStatus(Long docId, String status, String txHash) {
        if (docId == null) return;
        try {
            documentRepository.findById(docId).ifPresent(doc -> {
                doc.setStatus(status);
                if (txHash != null && !txHash.startsWith("Error")) {
                    doc.setTxHash(txHash);
                }
                documentRepository.save(doc);
            });
        } catch (Exception e) {
            logger.error("Failed to update document status", e);
        }
    }
}
