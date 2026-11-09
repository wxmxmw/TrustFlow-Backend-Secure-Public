package com.trustflow.backend.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "verification_logs")
public class VerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long verifyId;

    @Column(name = "verified_at", nullable = false)
    private OffsetDateTime verifiedAt;

    @Column(name = "input_type", nullable = false)
    private String inputType; // text/image

    @Column(name = "uploaded_file_path")
    private String uploadedFilePath;

    @Column(name = "extracted_watermark")
    private String extractedWatermark;

    @Column(name = "verifier_ip")
    private String verifierIp;

    @Column(name = "uploaded_content_hash", columnDefinition = "bpchar(64)")
    private String uploadedContentHash;

    @Column(name = "matched_tx_hash")
    private String matchedTxHash;

    @Column(name = "similarity_score")
    private Double similarityScore;

    @Column(name = "status", nullable = false)
    private String status; // success/no_watermark/tampered/error

    public Long getVerifyId() { return verifyId; }
    public void setVerifyId(Long verifyId) { this.verifyId = verifyId; }


    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }


    public OffsetDateTime getCreatedAt() { return verifiedAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.verifiedAt = createdAt; }

    public String getInputType() { return inputType; }
    public void setInputType(String inputType) { this.inputType = inputType; }

    public String getUploadedFilePath() { return uploadedFilePath; }
    public void setUploadedFilePath(String uploadedFilePath) { this.uploadedFilePath = uploadedFilePath; }

    public String getExtractedWatermark() { return extractedWatermark; }
    public void setExtractedWatermark(String extractedWatermark) { this.extractedWatermark = extractedWatermark; }

    public String getVerifierIp() { return verifierIp; }
    public void setVerifierIp(String verifierIp) { this.verifierIp = verifierIp; }

    public String getUploadedContentHash() { return uploadedContentHash; }
    public void setUploadedContentHash(String uploadedContentHash) { this.uploadedContentHash = uploadedContentHash; }

    public String getMatchedTxHash() { return matchedTxHash; }
    public void setMatchedTxHash(String matchedTxHash) { this.matchedTxHash = matchedTxHash; }

    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
