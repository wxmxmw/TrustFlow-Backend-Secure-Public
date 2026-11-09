package com.trustflow.backend.dto;

import java.time.OffsetDateTime;

public class DialogChainItemDto {
    private String tx_hash;
    private OffsetDateTime created_at;

    public DialogChainItemDto() {}

    public DialogChainItemDto(String txHash, OffsetDateTime createdAt) {
        this.tx_hash = txHash;
        this.created_at = createdAt;
    }

    public String getTx_hash() { return tx_hash; }
    public void setTx_hash(String tx_hash) { this.tx_hash = tx_hash; }

    public OffsetDateTime getCreated_at() { return created_at; }
    public void setCreated_at(OffsetDateTime created_at) { this.created_at = created_at; }
}
