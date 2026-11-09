package com.trustflow.backend.web;

import com.trustflow.backend.service.FileStorageService;
import com.trustflow.backend.service.DocumentIndexingService;
import com.trustflow.backend.service.ProofService;
import com.trustflow.backend.repo.DocumentRepository;
import com.trustflow.backend.repo.CitationRepository;
import com.trustflow.backend.repo.DocumentChunkRepository;
import com.trustflow.backend.repo.OnchainProofRepository;
import com.trustflow.backend.model.Document;
import com.trustflow.backend.model.DocumentChunk;
import com.trustflow.backend.core.JwtService;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsController {

    private static final Logger log = LoggerFactory.getLogger(DocumentsController.class);

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final JwtService jwtService;
    private final DocumentIndexingService documentIndexingService;
    private final CitationRepository citationRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ProofService proofService;
    private final OnchainProofRepository onchainProofRepository;

    public DocumentsController(FileStorageService fileStorageService,
                               DocumentRepository documentRepository,
                               JwtService jwtService,
                               DocumentIndexingService documentIndexingService,
                               CitationRepository citationRepository,
                               DocumentChunkRepository chunkRepository,
                               ProofService proofService,
                               OnchainProofRepository onchainProofRepository) {
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
        this.jwtService = jwtService;
        this.documentIndexingService = documentIndexingService;
        this.citationRepository = citationRepository;
        this.chunkRepository = chunkRepository;
        this.proofService = proofService;
        this.onchainProofRepository = onchainProofRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("file") MultipartFile file) {


        if (authorization == null || authorization.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        DecodedJWT jwt;
        try { jwt = jwtService.verify(token); } catch (Exception e) { return ResponseEntity.status(401).body(Map.of("error", "Invalid Token")); }
        Long userId = jwt.getClaim("user_id").asLong();

        try {

            FileStorageService.FileStorageResult result = fileStorageService.storeFile(file);


            Optional<Document> existingDoc = documentRepository.findByFileHash(result.getFileHash());
            if (existingDoc.isPresent() && existingDoc.get().getUserId().equals(userId)) {
                Map<String, Object> res = new LinkedHashMap<>();
                res.put("doc_id", existingDoc.get().getDocId());
                res.put("file_name", existingDoc.get().getFileName());
                res.put("status", existingDoc.get().getStatus());
                res.put("message", "Existing document reused");
                return ResponseEntity.ok(res);
            }


            Document document = new Document();
            document.setUserId(userId);
            document.setFileName(result.getFileName());
            document.setMimeType(result.getMimeType());
            document.setFileSize(result.getFileSize());
            document.setFilePath(result.getFilePath());
            document.setFileHash(result.getFileHash());
            document.setUploadTime(java.time.OffsetDateTime.now());
            document.setStatus("processing");
            document.setTxHash(null);

            Document savedDoc = documentRepository.save(document);

            // ==============================================================

            // ==============================================================
            CompletableFuture.runAsync(() -> {
                Long docId = savedDoc.getDocId();
                try {

                    documentIndexingService.indexDocument(docId);


                    Document freshDoc = documentRepository.findById(docId).orElse(null);

                    if (freshDoc != null && "indexed".equals(freshDoc.getStatus())) {
                        log.info("============== [Step 2] Indexing complete; submitting proof ID: {} ==============", docId);


                        proofService.submitDocumentProofAsync(
                                freshDoc.getFileHash(),
                                freshDoc.getDocId(),
                                onchainProofRepository
                        );
                    } else {
                        log.warn("Document indexing incomplete or failed; skipping proof. Status: {}", freshDoc == null ? "null" : freshDoc.getStatus());
                    }

                } catch (Exception e) {

                    log.error("Background document task failed, document ID: " + docId, e);
                    try {
                        Document errorDoc = documentRepository.findById(docId).orElse(null);

                        if (errorDoc != null && !"indexed".equals(errorDoc.getStatus()) && !"completed".equals(errorDoc.getStatus())) {
                            errorDoc.setStatus("failed");
                            documentRepository.save(errorDoc);
                        }
                    } catch (Exception ex) { /* ignore */ }
                }
            });

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("doc_id", savedDoc.getDocId());
            res.put("file_name", savedDoc.getFileName());
            res.put("status", "processing");
            res.put("message", "Upload accepted; processing in background...");
            return new ResponseEntity<>(res, HttpStatus.CREATED);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }


    @GetMapping
    public List<Map<String, Object>> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null) return new ArrayList<>();
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        try { if(jwtService.verify(token) == null) return new ArrayList<>(); } catch(Exception e) { return new ArrayList<>(); }
        Long userId = jwtService.verify(token).getClaim("user_id").asLong();

        return documentRepository.findByUserIdOrderByUploadTimeDesc(userId).stream().map(doc -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("doc_id", doc.getDocId());
            item.put("file_name", doc.getFileName());
            item.put("file_size", doc.getFileSize());
            item.put("upload_time", doc.getUploadTime());
            String status = doc.getStatus();
            if ("indexed".equals(status)) {
                status = "ready";
            }
            item.put("status", status);
            item.put("tx_hash", doc.getTxHash());
            return item;
        }).collect(Collectors.toList());
    }


    @DeleteMapping("/{doc_id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("doc_id") Long docId) {

        if (authorization == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        Long userId = null;
        try { userId = jwtService.verify(authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization).getClaim("user_id").asLong(); } catch(Exception e) {}
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        Optional<Document> docOpt = documentRepository.findById(docId);
        if (docOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        if (!docOpt.get().getUserId().equals(userId)) return ResponseEntity.status(403).body(Map.of("error", "Access denied"));

        try {
            List<DocumentChunk> chunks = chunkRepository.findByDocId(docId);
            List<Long> chunkIds = chunks.stream().map(DocumentChunk::getChunkId).collect(Collectors.toList());

            if (!chunkIds.isEmpty()) {
                citationRepository.deleteByChunkIdIn(chunkIds);
                chunkRepository.deleteByDocId(docId);
            }
            documentRepository.deleteById(docId);
            try { fileStorageService.deleteFile(docOpt.get().getFilePath()); } catch (Exception ignored) {}

            return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{docId}/preview")
    public ResponseEntity<Resource> preview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long docId) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            return ResponseEntity.status(401).build();
        Long userId;
        try {
            userId = jwtService.verify(authorization.substring(7)).getClaim("user_id").asLong();
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            Optional<Document> docOpt = documentRepository.findById(docId);
            if (docOpt.isEmpty()) return ResponseEntity.notFound().build();
            Document doc = docOpt.get();
            if (!doc.getUserId().equals(userId)) return ResponseEntity.status(404).build();
            Path filePath = Paths.get(doc.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/octet-stream";
                if(doc.getFileName().toLowerCase().endsWith(".pdf")) contentType="application/pdf";
                else if(doc.getFileName().toLowerCase().endsWith(".txt")) contentType="text/plain";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getFileName() + "\"")
                        .body(resource);
            }
        } catch (Exception e) {}
        return ResponseEntity.notFound().build();
    }
}
