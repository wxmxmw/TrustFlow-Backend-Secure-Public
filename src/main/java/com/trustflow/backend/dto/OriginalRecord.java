package com.trustflow.backend.dto;

import java.time.OffsetDateTime;

public class OriginalRecord {
    private Long record_id;
    private OffsetDateTime created_at;
    private String creator_wallet;
    private String prompt;

    public Long getRecord_id() { return record_id; }
    public void setRecord_id(Long record_id) { this.record_id = record_id; }

    public OffsetDateTime getCreated_at() { return created_at; }
    public void setCreated_at(OffsetDateTime created_at) { this.created_at = created_at; }

    public String getCreator_wallet() { return creator_wallet; }
    public void setCreator_wallet(String creator_wallet) { this.creator_wallet = creator_wallet; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
