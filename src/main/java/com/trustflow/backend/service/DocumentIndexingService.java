package com.trustflow.backend.service;

import com.trustflow.backend.model.Document;
import com.trustflow.backend.model.DocumentChunk;
import com.trustflow.backend.repo.DocumentChunkRepository;
import com.trustflow.backend.repo.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIndexingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentParser documentParser;
    private final EmbeddingService embeddingService;
    private final TransactionTemplate transactionTemplate;

    public DocumentIndexingService(DocumentRepository documentRepository,
                                   DocumentChunkRepository chunkRepository,
                                   DocumentParser documentParser,
                                   EmbeddingService embeddingService,
                                   PlatformTransactionManager transactionManager) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentParser = documentParser;
        this.embeddingService = embeddingService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }


    public void indexDocument(Long docId) {
        log.info("============== [Step 1] Indexing document ID: {} ==============", docId);

        Document document = documentRepository.findById(docId).orElse(null);
        if (document == null) {
            log.error("Document ID {} not found; indexing stopped", docId);
            return;
        }

        try {

            log.info("Parsing file: {}", document.getFileName());
            List<DocumentParser.TextChunk> parsedChunks = documentParser.parse(
                    document.getFilePath(),
                    document.getMimeType());


            if (parsedChunks.isEmpty()) {
                log.warn("Parsed file is empty: {}", document.getFileName());
                document.setStatus("indexed");
                documentRepository.save(document);
                return;
            }


            log.info("Generating embeddings for {} chunks...", parsedChunks.size());
            List<String> texts = parsedChunks.stream().map(DocumentParser.TextChunk::getText).toList();
            List<float[]> embeddings = embeddingService.generateBatchEmbeddings(texts);


            List<DocumentChunk> batchBuffer = new ArrayList<>();
            int batchSize = 50;

            for (int i = 0; i < parsedChunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocId(docId);
                chunk.setContentText(parsedChunks.get(i).getText());
                chunk.setPageNumber(parsedChunks.get(i).getPageNumber());
                chunk.setChunkHash(DocumentChunk.calculateChunkHash(parsedChunks.get(i).getText()));

                if (i < embeddings.size()) {
                    chunk.setEmbeddingVector(floatArrayToList(embeddings.get(i)));
                }

                batchBuffer.add(chunk);

                if (batchBuffer.size() >= batchSize) {
                    saveBatchAndVectors(batchBuffer);
                    batchBuffer.clear();
                }
            }

            if (!batchBuffer.isEmpty()) {
                saveBatchAndVectors(batchBuffer);
            }



            document = documentRepository.findById(docId).orElse(document);
            document.setStatus("indexed");


            if (document.getTxHash() != null && document.getTxHash().startsWith("Error")) {
                document.setTxHash(null);
            }

            documentRepository.save(document);
            log.info("============== [Step 1] Document indexed ID: {} ==============", docId);

        } catch (Exception e) {
            log.error("!!!!!!!! Document indexing failed ID: {} !!!!!!!!", docId, e);


            document = documentRepository.findById(docId).orElse(document);
            document.setStatus("failed");




            documentRepository.save(document);


            throw new RuntimeException("Indexing failed: " + e.getMessage(), e);
        }
    }

    protected void saveBatchAndVectors(List<DocumentChunk> chunks) {
        transactionTemplate.executeWithoutResult(status -> {
            List<DocumentChunk> savedChunks = chunkRepository.saveAll(chunks);
            chunkRepository.flush();
            for (DocumentChunk savedChunk : savedChunks) {
                List<Float> vector = savedChunk.getEmbeddingVector();
                if (vector != null && !vector.isEmpty()) {
                    String vectorStr = formatVectorToString(vector);
                    chunkRepository.updateEmbeddingVector(savedChunk.getChunkId(), vectorStr);
                }
            }
        });
    }

    private List<Float> floatArrayToList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add(f);
        return list;
    }

    private String formatVectorToString(List<Float> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}