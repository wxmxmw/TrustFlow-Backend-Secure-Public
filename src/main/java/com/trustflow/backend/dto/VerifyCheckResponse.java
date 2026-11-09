package com.trustflow.backend.dto;

import java.util.List;

public class VerifyCheckResponse {

    // success | risky | fail
    private String status;

    // semantic_similarity | invisible_watermark
    private String verification_type;

    // optional message for fail/risky
    private String message;

    // Case A
    private SemanticCheckResult check_result;
    private MatchedRecord matched_record;

    // Case B
    private WatermarkCheckResult watermark_check_result;
    private OriginalRecord original_record;

    // common
    private List<CitationDto> citations;
    private List<DialogChainItemDto> dialog_chain;
    private String blockchain_explorer_url;

    // ===== getters/setters =====

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVerification_type() { return verification_type; }
    public void setVerification_type(String verification_type) { this.verification_type = verification_type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public SemanticCheckResult getCheck_result() { return check_result; }
    public void setCheck_result(SemanticCheckResult check_result) { this.check_result = check_result; }

    public MatchedRecord getMatched_record() { return matched_record; }
    public void setMatched_record(MatchedRecord matched_record) { this.matched_record = matched_record; }

    public WatermarkCheckResult getWatermark_check_result() { return watermark_check_result; }
    public void setWatermark_check_result(WatermarkCheckResult watermark_check_result) { this.watermark_check_result = watermark_check_result; }

    public OriginalRecord getOriginal_record() { return original_record; }
    public void setOriginal_record(OriginalRecord original_record) { this.original_record = original_record; }

    public List<CitationDto> getCitations() { return citations; }
    public void setCitations(List<CitationDto> citations) { this.citations = citations; }

    public List<DialogChainItemDto> getDialog_chain() { return dialog_chain; }
    public void setDialog_chain(List<DialogChainItemDto> dialog_chain) { this.dialog_chain = dialog_chain; }

    public String getBlockchain_explorer_url() { return blockchain_explorer_url; }
    public void setBlockchain_explorer_url(String blockchain_explorer_url) { this.blockchain_explorer_url = blockchain_explorer_url; }
}
