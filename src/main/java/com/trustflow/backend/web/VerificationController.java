package com.trustflow.backend.web;

import com.trustflow.backend.model.*;
import com.trustflow.backend.repo.*;
import com.trustflow.backend.service.HashUtil;
import com.trustflow.backend.service.StorageService;
import com.trustflow.backend.service.TextSimilarityService;
import com.trustflow.backend.service.WatermarkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/verify")
public class VerificationController {

    private static final Logger log = LoggerFactory.getLogger(VerificationController.class);

    private final GenerationRecordRepository generationRecordRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final StorageService storageService;
    private final WatermarkService watermarkService;
    private final TextSimilarityService textSimilarityService;
    private final CitationRepository citationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    public VerificationController(
            GenerationRecordRepository generationRecordRepository,
            VerificationLogRepository verificationLogRepository,
            StorageService storageService,
            WatermarkService watermarkService,
            TextSimilarityService textSimilarityService,
            CitationRepository citationRepository,
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository) {
        this.generationRecordRepository = generationRecordRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.storageService = storageService;
        this.watermarkService = watermarkService;
        this.textSimilarityService = textSimilarityService;
        this.citationRepository = citationRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> check(
            @RequestParam("type") String type,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        if ("text".equalsIgnoreCase(type)) {

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", "text_required"));
            }

            String norm = HashUtil.normalizeForHash(text);
            String contentHash = HashUtil.sha256Hex(norm);

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("verification_type", "semantic_similarity");
            res.put("uploaded_content_hash", contentHash);

            Map<String, Object> check = new LinkedHashMap<>();
            Map<String, Object> matched = new LinkedHashMap<>();


            Optional<GenerationRecord> exact = generationRecordRepository.findTop1ByContentHash(contentHash);
            if (exact.isPresent()) {
                GenerationRecord gr = exact.get();

                check.put("similarity_score", 1.0);
                check.put("is_tampered", false);

                populateRecordData(matched, gr);
                List<Map<String, Object>> citationList = buildCitationList(gr.getRecordId());
                List<Map<String, Object>> dialogChain = buildDialogChain(gr);


                matched.put("citations", citationList);
                matched.put("dialog_chain", dialogChain);

                res.put("check_result", check);
                res.put("matched_record", matched);
                res.put("citations", citationList);
                res.put("dialog_chain", dialogChain);
                res.put("status", "success");
                res.put("blockchain_explorer_url", getExplorerUrl(gr.getTxHash()));

                saveVerifyLog("text", null, null, null, contentHash, gr.getTxHash(), 1.0, "success");
                return ResponseEntity.ok(res);
            }


            double best = 0.0;
            GenerationRecord bestRec = null;
            try {
                List<GenerationRecord> recent = generationRecordRepository
                        .findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .getContent();

                for (GenerationRecord gr : recent) {
                    String c = safeContent(gr);
                    if (c == null || c.isBlank())
                        continue;
                    double sim = textSimilarityService.charBigramCosine(norm, c);
                    double overlap = textSimilarityService.overlapCoefficient(norm, c);
                    sim = Math.max(sim, overlap);
                    if (sim > best) {
                        best = sim;
                        bestRec = gr;
                    }
                }
            } catch (Exception ignored) {
            }

            check.put("similarity_score", best);
            boolean pass = bestRec != null && best >= 0.60;
            check.put("is_tampered", !pass);

            if (pass) {
                populateRecordData(matched, bestRec);
                List<Map<String, Object>> citationList = buildCitationList(bestRec.getRecordId());
                List<Map<String, Object>> dialogChain = buildDialogChain(bestRec);

                matched.put("citations", citationList);
                matched.put("dialog_chain", dialogChain);

                res.put("check_result", check);
                res.put("matched_record", matched);
                res.put("citations", citationList);
                res.put("dialog_chain", dialogChain);
                res.put("status", "success");
                res.put("blockchain_explorer_url", getExplorerUrl(bestRec.getTxHash()));

                saveVerifyLog("text", null, null, null, contentHash, bestRec.getTxHash(), best, "success");
                return ResponseEntity.ok(res);
            }


            res.put("status", "fail");
            res.put("check_result", check);
            res.put("message", "no_match");
            saveVerifyLog("text", null, null, null, contentHash, null, best, "no_match");
            return ResponseEntity.ok(res);

        } else if ("image".equalsIgnoreCase(type)) {

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("verification_type", "invisible_watermark");
            Map<String, Object> check = new LinkedHashMap<>();

            if (file == null || file.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("message", "file_required"));

            try {
                byte[] bytes = file.getBytes();
                WatermarkService.ExtractResult ex = watermarkService.extractFromPng(bytes);

                check.put("has_watermark", ex.ok());
                check.put("extracted_data", ex.payload());
                res.put("check_result", check);

                if (!ex.ok()) {
                    res.put("status", "fail");
                    return ResponseEntity.ok(res);
                }

                String txHash = ex.payload();
                Optional<GenerationRecord> rec = generationRecordRepository.findByTxHash(txHash);
                if (rec.isEmpty() && txHash != null)
                    rec = generationRecordRepository.findTopByWatermarkInfoContaining(txHash);

                check.put("chain_record_found", rec.isPresent());

                if (rec.isPresent()) {
                    Map<String, Object> original = new LinkedHashMap<>();
                    populateRecordData(original, rec.get());
                    res.put("original_record", original);
                    res.put("status", "success");
                    res.put("blockchain_explorer_url", getExplorerUrl(rec.get().getTxHash()));
                    saveVerifyLog("image", null, txHash, null, null, txHash, null, "success");
                } else {
                    res.put("status", "fail");
                    saveVerifyLog("image", null, txHash, null, null, txHash, null, "tx_not_found");
                }
                return ResponseEntity.ok(res);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
            }
        }
        return ResponseEntity.badRequest().body(Map.of("message", "unsupported_type"));
    }


    @GetMapping("/tx/{tx_hash}")
    public Map<String, Object> byTx(@PathVariable("tx_hash") String txHash) {
        Map<String, Object> res = new LinkedHashMap<>();
        Optional<GenerationRecord> rec = generationRecordRepository.findByTxHash(txHash);
        if (rec.isPresent()) {
            GenerationRecord gr = rec.get();
            populateRecordData(res, gr);
            res.put("citations", buildCitationList(gr.getRecordId()));
            res.put("dialog_chain", buildDialogChain(gr));
            res.put("blockchain_explorer_url", getExplorerUrl(gr.getTxHash()));
        } else {
            res.put("status", "fail");
        }
        return res;
    }


    private List<Map<String, Object>> buildCitationList(Long recordId) {
        if (recordId == null)
            return Collections.emptyList();
        List<Citation> citations = citationRepository.findByRecordId(recordId);
        if (citations == null)
            return Collections.emptyList();

        return citations.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("doc_id", c.getDocId());
            map.put("score", c.getScore());


            String fileName = "Unknown File";
            Integer page = 1;


            try {
                Optional<Document> doc = documentRepository.findById(c.getDocId());
                if (doc.isPresent()) {
                    fileName = doc.get().getFileName();
                    map.put("file_hash", doc.get().getFileHash());
                }
            } catch (Exception e) {
                log.warn("Error fetching document: {}", e.getMessage());
            }
            map.put("file_name", fileName);


            String snippet = "";
            try {
                Optional<DocumentChunk> chunk = chunkRepository.findById(c.getChunkId());
                if (chunk.isPresent()) {
                    page = chunk.get().getPageNumber();
                    String raw = chunk.get().getContentText();
                    snippet = (raw != null && raw.length() > 50) ? raw.substring(0, 50) + "..." : raw;
                    map.put("chunk_hash", chunk.get().getChunkHash());
                }
            } catch (Exception e) {
                log.warn("Error fetching chunk: {}", e.getMessage());
            }
            map.put("page", page);
            map.put("text_snippet", snippet);



            String fileUrl = "/api/v1/documents/" + c.getDocId() + "/preview";


            map.put("url", fileUrl + "#page=" + page);
            map.put("download_url", fileUrl);

            return map;
        }).collect(Collectors.toList());
    }


    private List<Map<String, Object>> buildDialogChain(GenerationRecord current) {
        List<Map<String, Object>> chain = new ArrayList<>();
        int depth = 0;
        GenerationRecord ptr = current;
        while (ptr.getParentTxHash() != null && !ptr.getParentTxHash().isEmpty() && depth < 10) {
            Optional<GenerationRecord> parent = generationRecordRepository.findByTxHash(ptr.getParentTxHash());
            if (parent.isEmpty())
                break;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tx_hash", parent.get().getTxHash());
            item.put("created_at", parent.get().getCreatedAt());
            chain.add(0, item);
            ptr = parent.get();
            depth++;
        }
        return chain;
    }

    private void populateRecordData(Map<String, Object> map, GenerationRecord gr) {
        map.put("record_id", gr.getRecordId());
        map.put("model_name", gr.getModelName());
        map.put("created_at", gr.getCreatedAt());
        map.put("content_preview", preview(gr.getContent()));
        map.put("tx_hash", gr.getTxHash());
    }

    private String getExplorerUrl(String tx) {
        return (tx != null && tx.startsWith("0x")) ? "https://sepolia.etherscan.io/tx/" + tx : null;
    }

    private String preview(String s) {
        if (s == null)
            return "";
        return s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }

    private String safeContent(GenerationRecord gr) {
        return gr != null ? gr.getContent() : null;
    }

    private Long safeRecordId(GenerationRecord gr) {
        return gr != null ? gr.getRecordId() : null;
    }

    private Long safeSessionId(GenerationRecord gr) {
        return gr != null ? gr.getSessionId() : null;
    }

    private String safeTxHash(GenerationRecord gr) {
        return gr != null ? gr.getTxHash() : null;
    }

    private String safeContentHash(GenerationRecord gr) {
        return gr != null ? gr.getContentHash() : null;
    }

    private String safeModelName(GenerationRecord gr) {
        return gr != null ? gr.getModelName() : null;
    }

    private String safeCreatedAt(GenerationRecord gr) {
        return gr != null ? gr.getCreatedAt().toString() : null;
    }

    private String safeArtifactUrl(GenerationRecord gr) {
        return gr != null ? gr.getArtifactUrl() : null;
    }

    private void saveVerifyLog(String type, String path, String wm, String ip, String chash, String tx, Double score,
            String status) {
        try {
            VerificationLog log = new VerificationLog();
            log.setInputType(type);
            log.setUploadedFilePath(path);
            log.setExtractedWatermark(wm);
            log.setVerifierIp(ip);
            log.setUploadedContentHash(chash);
            log.setMatchedTxHash(tx);
            log.setSimilarityScore(score);
            log.setStatus(status);
            log.setCreatedAt(OffsetDateTime.now());
            verificationLogRepository.save(log);
        } catch (Exception ignored) {
        }
    }
}
