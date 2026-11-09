package com.trustflow.backend.web;

import com.trustflow.backend.dto.ChatCompletionRequest;
import com.trustflow.backend.core.JwtService;
import com.trustflow.backend.service.LlmService;
import com.trustflow.backend.service.RetrievalService;
import com.trustflow.backend.repo.CitationRepository;
import com.trustflow.backend.repo.DocumentRepository;
import com.trustflow.backend.repo.DocumentChunkRepository;
import com.trustflow.backend.model.DocumentChunk;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.trustflow.backend.model.GenerationRecord;
import com.trustflow.backend.model.Session;
import com.trustflow.backend.model.Citation;
import com.trustflow.backend.repo.GenerationRecordRepository;
import com.trustflow.backend.repo.OnchainProofRepository;
import com.trustflow.backend.repo.SessionRepository;
import com.trustflow.backend.service.ProofService;
import com.trustflow.backend.service.FileStorageService;
import com.trustflow.backend.service.ImageGenerationService;
import com.trustflow.backend.service.WatermarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final SessionRepository sessionRepository;
    private final GenerationRecordRepository generationRecordRepository;
    private final JwtService jwtService;
    private final ProofService proofService;
    private final OnchainProofRepository onchainProofRepository;
    private final LlmService llmService;
    private final RetrievalService retrievalService;
    private final CitationRepository citationRepository;
    private final FileStorageService fileStorageService;
    private final ImageGenerationService imageGenerationService;
    private final WatermarkService watermarkService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    public ChatController(SessionRepository sessionRepository,
                          GenerationRecordRepository generationRecordRepository,
                          JwtService jwtService,
                          LlmService llmService,
                          RetrievalService retrievalService,
                          CitationRepository citationRepository,
                          FileStorageService fileStorageService,
                          ImageGenerationService imageGenerationService,
                          ProofService proofService,
                          OnchainProofRepository onchainProofRepository,
                          WatermarkService watermarkService,
                          DocumentRepository documentRepository,
                          DocumentChunkRepository chunkRepository) {
        this.sessionRepository = sessionRepository;
        this.generationRecordRepository = generationRecordRepository;
        this.jwtService = jwtService;
        this.llmService = llmService;
        this.retrievalService = retrievalService;
        this.citationRepository = citationRepository;
        this.fileStorageService = fileStorageService;
        this.imageGenerationService = imageGenerationService;
        this.proofService = proofService;
        this.onchainProofRepository = onchainProofRepository;
        this.watermarkService = watermarkService;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    // ==========================================

    // ==========================================
    @PostMapping("/completions")
    public ResponseEntity<Map<String, Object>> completions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ChatCompletionRequest req) {


        if (authorization == null || authorization.isEmpty())
            return ResponseEntity.status(401).body(Map.of("error", "Missing Authorization header"));
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        DecodedJWT jwt;
        try {
            jwt = jwtService.verify(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid Token"));
        }
        Long userId = jwt.getClaim("user_id").asLong();

        Optional<Session> sOpt = sessionRepository.findById(req.session_id);
        if (sOpt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
        Session session = sOpt.get();
        if (!session.getUserId().equals(userId))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));

        ObjectMapper om = new ObjectMapper();
        String paramsJson = null;
        try {
            paramsJson = req.parameters != null ? om.writeValueAsString(req.parameters) : null;
        } catch (Exception ignored) {
        }

        String mode = req.mode != null ? req.mode.toLowerCase() : "text";
        String contentType = "text".equals(mode) ? "text" : "image";


        OffsetDateTime userTime = OffsetDateTime.now();


        GenerationRecord grUser = new GenerationRecord();
        grUser.setSessionId(session.getSessionId());
        grUser.setRole("user");
        grUser.setContentType("text");
        grUser.setContent(req.prompt != null ? req.prompt : "");
        grUser.setModelName(req.model);
        grUser.setParameters(paramsJson);
        grUser.setCreatedAt(userTime);
        generationRecordRepository.save(grUser);


        String generatedContent = "";
        String artifactUrl = null;
        String contentHash = null;
        String txHashForWatermark = null;
        List<RetrievalService.RetrievedChunk> retrievedChunks = Collections.emptyList();

        if ("image".equals(contentType)) {
            try {
                generatedContent = req.prompt != null ? req.prompt : "";
                txHashForWatermark = "0ximg_" + UUID.randomUUID().toString().replace("-", "");
                byte[] baseImageBytes = watermarkService.mockGeneratePng(generatedContent);
                try {
                    if (!req.model.contains("mock")) {
                        ImageGenerationService.GeneratedImage img = imageGenerationService
                                .generateImage(generatedContent, req.model, req.parameters);
                        baseImageBytes = img.getBytes();
                    }
                } catch (Exception ignored) {
                }
                byte[] watermarked = watermarkService.embedToPng(baseImageBytes, txHashForWatermark);
                FileStorageService.FileStorageResult stored = fileStorageService.storeImageBytes(watermarked,
                        "generated.png", "image/png");
                artifactUrl = stored.getFilePath();
                contentHash = stored.getFileHash();
            } catch (Exception e) {
                return ResponseEntity.status(502).body(Map.of("error", "Image gen failed"));
            }
        } else {

            try {
                retrievedChunks = retrievalService.retrieveTopK(req.prompt, userId);
            } catch (Exception ignored) {
            }

            String generatedInput = req.prompt;
            if (!retrievedChunks.isEmpty()) {
                StringBuilder sb = new StringBuilder("Answer using the following context:\n\n");
                for (RetrievalService.RetrievedChunk c : retrievedChunks)
                    sb.append(c.getContent()).append("\n\n");
                sb.append("Question: ").append(req.prompt);
                generatedInput = sb.toString();
            }
            try {

                List<LlmService.Message> messages = new ArrayList<>();


                List<GenerationRecord> history = generationRecordRepository
                        .findBySessionIdOrderByCreatedAtDesc(session.getSessionId());
                List<GenerationRecord> recentHistory = history.stream()
                        .filter(r -> "text".equals(r.getContentType()))
                        .limit(10)
                        .sorted(java.util.Comparator.comparing(GenerationRecord::getCreatedAt))
                        .toList();


                for (GenerationRecord r : recentHistory) {
                    messages.add(new LlmService.Message(r.getRole(), r.getContent()));
                }


                messages.add(new LlmService.Message("user", generatedInput));

                generatedContent = llmService.generateCompletion(messages);
            } catch (Exception e) {
                generatedContent = "Error generating response.";
            }
        }


        OffsetDateTime botTime = OffsetDateTime.now();
        if (!botTime.isAfter(userTime)) {
            botTime = userTime.plusSeconds(1);
        }

        Optional<GenerationRecord> lastAssistant = generationRecordRepository
                .findTopBySessionIdAndRoleOrderByCreatedAtDesc(session.getSessionId(), "assistant");

        GenerationRecord gr = new GenerationRecord();
        gr.setSessionId(session.getSessionId());
        gr.setRole("assistant");
        gr.setContentType(contentType);
        gr.setParentTxHash(lastAssistant.map(GenerationRecord::getTxHash).orElse(null));
        gr.setContent(generatedContent);
        gr.setArtifactUrl(artifactUrl);
        gr.setModelName(req.model);
        gr.setParameters(paramsJson);
        gr.setContentHash(contentHash);

        if (txHashForWatermark != null) {
            try {
                Map<String, String> wmInfo = new LinkedHashMap<>();
                wmInfo.put("tx_hash", txHashForWatermark);
                gr.setWatermarkInfo(om.writeValueAsString(wmInfo));
            } catch (Exception ignored) {
            }
        }

        gr.setCreatedAt(botTime);

        GenerationRecord saved;
        try {
            saved = generationRecordRepository.save(gr);
        } catch (Exception e) {
            log.error("Failed to save generation record", e);
            return ResponseEntity.status(500).body(Map.of("error", "Database error: " + e.getMessage()));
        }


        proofService.submitProofAsync(
                        generatedContent, saved.getRecordId(), onchainProofRepository)
                .thenAccept(txHashResult -> {
                    if (txHashResult != null) {
                        saved.setTxHash(txHashResult);
                        generationRecordRepository.save(saved);
                        session.setLatestTxHash(txHashResult);
                        sessionRepository.save(session);
                        log.info("Chat record onchain proof updated, recordId: {}, txHash: {}",
                                saved.getRecordId(), txHashResult);
                    } else {
                        log.warn("Chat record onchain proof failed, recordId: {}", saved.getRecordId());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error updating chat record txHash, recordId: {}", saved.getRecordId(), ex);
                    return null;
                });


        Map<String, Object> data = new LinkedHashMap<>();
        data.put("record_id", saved.getRecordId());
        data.put("role", "assistant");
        data.put("content", generatedContent);
        data.put("content_type", contentType);
        data.put("created_at", saved.getCreatedAt());
        if (artifactUrl != null)
            data.put("artifact_url", artifactUrl);

        List<Map<String, Object>> citationList = new ArrayList<>();
        if ("text".equals(contentType) && !retrievedChunks.isEmpty()) {
            List<Citation> citationsToSave = new ArrayList<>();

            for (RetrievalService.RetrievedChunk chunk : retrievedChunks) {

                Citation dbCitation = new Citation();
                dbCitation.setRecordId(saved.getRecordId());
                dbCitation.setDocId(chunk.getDocId());
                dbCitation.setChunkId(chunk.getChunkId());
                dbCitation.setScore(chunk.getSimilarityScore() != null ? chunk.getSimilarityScore().doubleValue() : 0.0);
                citationsToSave.add(dbCitation);

                // ==============================================================


                // ==============================================================
                Double scoreVal = chunk.getSimilarityScore() != null ? chunk.getSimilarityScore().doubleValue() : 0.0;
                Map<String, Object> citation = buildCitationMap(chunk.getDocId(), chunk.getChunkId(), scoreVal);


                citation.put("file_hash", chunk.getFileHash());
                citation.put("chunk_hash", chunk.getChunkHash());

                String rawContent = chunk.getContent();
                if (rawContent != null) {
                    String cleanContent = rawContent.replaceAll("[\\r\\n]+", " ");
                    String snippet = cleanContent.length() > 30 ? cleanContent.substring(0, 30) + "..." : cleanContent;
                    citation.put("content_snippet", snippet);
                }

                citationList.add(citation);
            }


            if (!citationsToSave.isEmpty()) {
                try {
                    citationRepository.saveAll(citationsToSave);
                    citationRepository.flush();
                    log.info("✅ Citations saved and flushed. Count: {}", citationsToSave.size());
                } catch (Exception e) {
                    log.error("❌ Failed to save citations", e);

                }
            }
        }

        data.put("citations", citationList);
        data.put("tx_hash", "0xPENDING...");

        return ResponseEntity.ok(data);
    }

    // ==========================================

// ==========================================
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long sessionId) {

        if (authorization == null || !authorization.startsWith("Bearer "))
            return ResponseEntity.status(401).build();

        Long userId;
        try {
            userId = jwtService.verify(authorization.substring(7)).getClaim("user_id").asLong();
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
        if (userId == null) return ResponseEntity.status(401).build();
        Optional<Session> session = sessionRepository.findById(sessionId);
        if (session.isEmpty() || !session.get().getUserId().equals(userId))
            return ResponseEntity.status(404).build();

        List<GenerationRecord> records = generationRecordRepository.findBySessionId(sessionId);

        records.sort(Comparator.comparing(GenerationRecord::getRecordId));

        List<Map<String, Object>> history = records.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("record_id", r.getRecordId());
            map.put("role", r.getRole());
            map.put("content", r.getContent());
            map.put("content_type", r.getContentType());
            map.put("artifact_url", r.getArtifactUrl());
            map.put("tx_hash", r.getTxHash());
            map.put("created_at", r.getCreatedAt());




            List<Citation> dbCitations = citationRepository.findByRecordId(r.getRecordId());

            if (dbCitations != null && !dbCitations.isEmpty()) {

                log.info("🔍 Record ID {} found {} citations in DB.", r.getRecordId(), dbCitations.size());

                List<Map<String, Object>> citList = dbCitations.stream()
                        .map(c -> {

                            Map<String, Object> cMap = buildCitationMap(c.getDocId(), c.getChunkId(), c.getScore());

                            if (!cMap.containsKey("file_name")) {
                                cMap.put("file_name", "Unknown Doc (ID " + c.getDocId() + ")");
                            }
                            return cMap;
                        })
                        .collect(Collectors.toList());

                map.put("citations", citList);
            } else {

                map.put("citations", Collections.emptyList());
            }

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    // ==========================================

    // ==========================================
    private Map<String, Object> buildCitationMap(Long docId, Long chunkId, Double score) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("doc_id", docId);
        map.put("chunk_id", chunkId);
        map.put("score", score);

        try {
            documentRepository.findById(docId).ifPresent(doc -> map.put("file_name", doc.getFileName()));
        } catch (Exception ignored) {
        }

        try {
            chunkRepository.findById(chunkId).ifPresent(chunk -> {
                map.put("page", chunk.getPageNumber());
                String raw = chunk.getContentText();
                if (raw != null) {
                    map.put("text_snippet", raw.length() > 50 ? raw.substring(0, 50) + "..." : raw);
                }
            });
        } catch (Exception ignored) {
        }

        Integer pg = (Integer) map.getOrDefault("page", 1);
        map.put("url", "/api/v1/documents/" + docId + "/preview#page=" + pg);
        map.put("download_url", "/api/v1/documents/" + docId + "/preview");

        return map;
    }
}
