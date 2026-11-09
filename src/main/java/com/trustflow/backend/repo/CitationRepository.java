package com.trustflow.backend.repo;

import com.trustflow.backend.model.Citation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitationRepository extends JpaRepository<Citation, Long> {
    List<Citation> findByRecordId(Long recordId);

    @Modifying
    @Query("DELETE FROM Citation c WHERE c.chunkId IN :chunkIds")
    void deleteByChunkIdIn(@Param("chunkIds") List<Long> chunkIds);
}