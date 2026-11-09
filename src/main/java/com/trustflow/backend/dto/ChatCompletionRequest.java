package com.trustflow.backend.dto;

import java.util.Map;

public class ChatCompletionRequest {
    public Long session_id;
    public String mode;
    public String model;
    public Map<String, Object> parameters;
    public String prompt;
}

