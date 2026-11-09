package com.trustflow.backend.repo;

import com.trustflow.backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserIdOrderByUploadTimeDesc(Long userId);

    @Query("SELECT d FROM Document d WHERE d.fileHash = :fileHash")
    Optional<Document> findByFileHash(@Param("fileHash") String fileHash);
}