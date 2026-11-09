package com.trustflow.backend.web;

import com.trustflow.backend.dto.CreateSessionRequest;
import com.trustflow.backend.core.JwtService;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.trustflow.backend.model.Session;
import com.trustflow.backend.model.GenerationRecord;
import com.trustflow.backend.repo.SessionRepository;
import com.trustflow.backend.repo.GenerationRecordRepository;
import com.trustflow.backend.repo.CitationRepository;
import com.trustflow.backend.repo.DocumentRepository;
import com.trustflow.backend.repo.DocumentChunkRepository;
import com.trustflow.backend.dto.CitationDto;
import com.trustflow.backend.model.Citation;
import com.trustflow.backend.model.Document;
import com.trustflow.backend.model.DocumentChunk;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {
    private final SessionRepository sessionRepository;
    private final GenerationRecordRepository generationRecordRepository;
    private final CitationRepository citationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final JwtService jwtService;

    public SessionController(SessionRepository sessionRepository, GenerationRecordRepository generationRecordRepository,
            CitationRepository citationRepository, DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository, JwtService jwtService) {
        this.sessionRepository = sessionRepository;
        this.generationRecordRepository = generationRecordRepository;
        this.citationRepository = citationRepository;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        DecodedJWT jwt;
        try {
            jwt = jwtService.verify(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }
        Long userId = jwt.getClaim("user_id").asLong();
        if (userId == null) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }
        List<Session> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Session s : sessions) {
            Optional<GenerationRecord> last = generationRecordRepository
                    .findTopBySessionIdOrderByCreatedAtDesc(s.getSessionId());
            String lastActive = (last.isPresent() ? last.get().getCreatedAt() : s.getCreatedAt()).toString();
            Map<String, Object> item = new HashMap<>();
            item.put("session_id", s.getSessionId());
            item.put("title", s.getTitle());
            item.put("last_active", lastActive);
            res.add(item);
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) CreateSessionRequest req) {
        if (authorization == null || authorization.isEmpty()) {
            return ResponseEntity.status(401).body(new HashMap<>());
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        DecodedJWT jwt;
        try {
            jwt = jwtService.verify(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new HashMap<>());
        }
        Long userId = jwt.getClaim("user_id").asLong();
        if (userId == null) {
            return ResponseEntity.status(401).body(new HashMap<>());
        }
        String title = (req != null && req.title != null && !req.title.trim().isEmpty()) ? req.title : "Untitled session";
        OffsetDateTime now = OffsetDateTime.now();
        Session s = new Session();
        s.setUserId(userId);
        s.setTitle(title);
        s.setLatestTxHash(null);
        s.setCreatedAt(now);
        Session saved = sessionRepository.save(s);
        Map<String, Object> data = new HashMap<>();
        data.put("session_id", saved.getSessionId());
        data.put("title", saved.getTitle());
        data.put("latest_tx_hash", saved.getLatestTxHash());
        data.put("created_at", saved.getCreatedAt().toString());
        return ResponseEntity.ok(data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Long id) {
        Map<String, Object> res = new HashMap<>();
        res.put("message", "not_implemented");
        res.put("id", id);
        return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
    }

    @GetMapping("/{session_id}/history")
    public ResponseEntity<List<Map<String, Object>>> history(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("session_id") Long sessionId) {
        if (authorization == null || authorization.isEmpty()) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        DecodedJWT jwt;
        try {
            jwt = jwtService.verify(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }
        Long userId = jwt.getClaim("user_id").asLong();
        if (userId == null) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }
        Optional<Session> sOpt = sessionRepository.findById(sessionId);
        if (sOpt.isEmpty() || !sOpt.get().getUserId().equals(userId)) {
            return ResponseEntity.status(404).body(new ArrayList<>());
        }
        List<GenerationRecord> records = generationRecordRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (GenerationRecord gr : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", gr.getRole());
            item.put("content_type", gr.getContentType());
            item.put("content", gr.getContent());
            item.put("created_at", gr.getCreatedAt().toString());
            item.put("content_hash", gr.getContentHash());
            item.put("parent_tx_hash", gr.getParentTxHash());
            if ("assistant".equalsIgnoreCase(gr.getRole())) {
                item.put("tx_hash", gr.getTxHash());
                List<Citation> cites = citationRepository.findByRecordId(gr.getRecordId());
                List<CitationDto> citeDtos = new ArrayList<>();
                for (Citation c : cites) {
                    CitationDto cd = new CitationDto();
                    cd.setDoc_id(c.getDocId());
                    cd.setChunk_id(c.getChunkId());
                    cd.setScore(c.getScore());
                    Document d = documentRepository.findById(c.getDocId()).orElse(null);
                    if (d != null) {
                        cd.setFile_name(d.getFileName());
                    }
                    DocumentChunk dc = documentChunkRepository.findById(c.getChunkId()).orElse(null);
                    if (dc != null) {
                        cd.setPage(dc.getPageNumber());
                        String text = dc.getContentText();
                        if (text != null) {
                            String t = text.length() > 200 ? text.substring(0, 200) : text;
                            cd.setText_snippet(t);
                            String normalized = text.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ")
                                    .trim();
                            String cs = normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
                            cd.setContent_snippet(cs);
                        }
                    }
                    String base = "/api/v1/documents/" + c.getDocId() + "/preview";
                    Integer pageNum = dc != null ? dc.getPageNumber() : null;
                    cd.setUrl(pageNum != null ? (base + "#page=" + pageNum) : base);
                    cd.setDownload_url(base);
                    if (d != null) {
                        cd.setFile_hash(d.getFileHash());
                    }
                    if (dc != null) {
                        cd.setChunk_hash(dc.getChunkHash());
                    }
                    citeDtos.add(cd);
                }
                item.put("citations", citeDtos);
                if ("image".equalsIgnoreCase(gr.getContentType())) {
                    item.put("artifact_url", gr.getArtifactUrl());
                }
            }
            res.add(item);
        }
        return ResponseEntity.ok(res);
    }
}
