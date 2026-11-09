package com.trustflow.backend.model;

import jakarta.persistence.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long chunkId;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "page_number")
    private Integer pageNumber;

    /**


     *


     *


     */
    @Transient
    private List<Float> embeddingVector;

    @Column(name = "chunk_hash", nullable = false, length = 64)
    private String chunkHash;



    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }


    public List<Float> getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(List<Float> embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public String getChunkHash() {
        return chunkHash;
    }

    public void setChunkHash(String chunkHash) {
        this.chunkHash = chunkHash;
    }

    // --- Utility Methods ---

    public static String calculateChunkHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}