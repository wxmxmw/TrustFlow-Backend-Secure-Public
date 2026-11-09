package com.trustflow.backend.dto;

import java.time.OffsetDateTime;

public class MatchedRecord {
    private Long record_id;
    private Long session_id;
    private String tx_hash;
    private String parent_tx_hash;
    private String content_hash;
    private OffsetDateTime created_at;
    private String model_name;
    private String original_content;

    public Long getRecord_id() { return record_id; }
    public void setRecord_id(Long record_id) { this.record_id = record_id; }

    public Long getSession_id() { return session_id; }
    public void setSession_id(Long session_id) { this.session_id = session_id; }

    public String getTx_hash() { return tx_hash; }
    public void setTx_hash(String tx_hash) { this.tx_hash = tx_hash; }

    public String getParent_tx_hash() { return parent_tx_hash; }
    public void setParent_tx_hash(String parent_tx_hash) { this.parent_tx_hash = parent_tx_hash; }

    public String getContent_hash() { return content_hash; }
    public void setContent_hash(String content_hash) { this.content_hash = content_hash; }

    public OffsetDateTime getCreated_at() { return created_at; }
    public void setCreated_at(OffsetDateTime created_at) { this.created_at = created_at; }

    public String getModel_name() { return model_name; }
    public void setModel_name(String model_name) { this.model_name = model_name; }

    public String getOriginal_content() { return original_content; }
    public void setOriginal_content(String original_content) { this.original_content = original_content; }
}
