package com.trustflow.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "onchain_proof")
public class OnchainProof {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "generation_record_id")
    private Long generationRecordId;

    @Column(name = "content_hash", nullable = false, length = 66)
    private String contentHash;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "contract_address", length = 42)
    private String contractAddress;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getGenerationRecordId() {
        return generationRecordId;
    }

    public void setGenerationRecordId(Long generationRecordId) {
        this.generationRecordId = generationRecordId;
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

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
