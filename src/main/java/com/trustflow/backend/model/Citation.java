package com.trustflow.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "citations")
public class Citation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "citation_id")
    private Long citationId;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    @Column(name = "score", nullable = false)
    private Double score;

    // ==========================

    // ==========================
    public Citation() {
    }

    public Citation(Long recordId, Long docId, Long chunkId, Double score) {
        this.recordId = recordId;
        this.docId = docId;
        this.chunkId = chunkId;
        this.score = score;
    }

    // ==========================
    // Getters and Setters
    // ==========================

    public Long getCitationId() {
        return citationId;
    }

    public void setCitationId(Long citationId) {
        this.citationId = citationId;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    // ==========================

    // ==========================
    @Override
    public String toString() {
        return "Citation{" +
                "citationId=" + citationId +
                ", recordId=" + recordId +
                ", docId=" + docId +
                ", chunkId=" + chunkId +
                ", score=" + score +
                '}';
    }
}