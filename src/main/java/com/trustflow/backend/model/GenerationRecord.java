package com.trustflow.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name = "generation_records")
public class GenerationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "content_type", nullable = false, length = 16)
    private String contentType;

    @Column(name = "parent_tx_hash")
    private String parentTxHash;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "artifact_url")
    private String artifactUrl;

    @Column(name = "model_name")
    private String modelName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "JSON")
    private String parameters;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "tx_hash")
    private String txHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "watermark_info", columnDefinition = "JSON")
    private String watermarkInfo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getParentTxHash() {
        return parentTxHash;
    }

    public void setParentTxHash(String parentTxHash) {
        this.parentTxHash = parentTxHash;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getArtifactUrl() {
        return artifactUrl;
    }

    public void setArtifactUrl(String artifactUrl) {
        this.artifactUrl = artifactUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getWatermarkInfo() {
        return watermarkInfo;
    }

    public void setWatermarkInfo(String watermarkInfo) {
        this.watermarkInfo = watermarkInfo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}