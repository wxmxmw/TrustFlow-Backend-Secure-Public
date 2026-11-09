package com.trustflow.backend.dto;

public class WatermarkCheckResult {
    private boolean has_watermark;
    private String extracted_data;
    private boolean chain_record_found;

    public boolean isHas_watermark() { return has_watermark; }
    public void setHas_watermark(boolean has_watermark) { this.has_watermark = has_watermark; }

    public String getExtracted_data() { return extracted_data; }
    public void setExtracted_data(String extracted_data) { this.extracted_data = extracted_data; }

    public boolean isChain_record_found() { return chain_record_found; }
    public void setChain_record_found(boolean chain_record_found) { this.chain_record_found = chain_record_found; }
}
