package com.trustflow.backend.repo;

import com.trustflow.backend.model.GenerationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenerationRecordRepository extends JpaRepository<GenerationRecord, Long> {
    Optional<GenerationRecord> findTopBySessionIdOrderByCreatedAtDesc(Long sessionId);

    Optional<GenerationRecord> findTopBySessionIdAndRoleOrderByCreatedAtDesc(Long sessionId, String role);

    List<GenerationRecord> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    List<GenerationRecord> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    Optional<GenerationRecord> findByTxHash(String txHash);

    Optional<GenerationRecord> findTop1ByContentHash(String contentHash);

    Optional<GenerationRecord> findTopByTxHashOrderByCreatedAtDesc(String txHash);

    List<GenerationRecord> findBySessionId(Long sessionId);



    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM generation_records WHERE CAST(watermark_info AS TEXT) LIKE CONCAT('%', :watermarkInfoSnippet, '%') LIMIT 1", nativeQuery = true)
    Optional<GenerationRecord> findTopByWatermarkInfoContaining(
            @org.springframework.data.repository.query.Param("watermarkInfoSnippet") String watermarkInfoSnippet);
}
