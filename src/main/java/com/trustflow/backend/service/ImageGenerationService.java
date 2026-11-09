package com.trustflow.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.List;

@Service
public class ImageGenerationService {
    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;
    private final String defaultFormat = "png";
    private final String model;

    public ImageGenerationService(
            @Value("${app.image.api-url:}") String apiUrl,
            @Value("${app.image.api-key:}") String apiKey,
            @Value("${app.image.model:}") String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = (apiUrl == null || apiUrl.isBlank())
                ? null
                : WebClient.builder()
                        .baseUrl(apiUrl)
                        .defaultHeader("Authorization", "Bearer " + apiKey)
                        .defaultHeader("Content-Type", "application/json")
                        .build();
    }

    public GeneratedImage generateImage(String prompt) {
        return generateImage(prompt, null, null);
    }

    public GeneratedImage generateImage(String prompt, String modelOverride) {
        return generateImage(prompt, modelOverride, null);
    }

    public GeneratedImage generateImage(String prompt, String modelOverride, Map<String, Object> params) {
        if (webClient == null) {
            throw new IllegalStateException("image_api_url_not_configured");
        }
        String useModel = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : this.model;
        ModelRequest request = new ModelRequest(prompt, useModel, params);
        ModelResponse response;
        try {
            response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ModelResponse.class)
                    .block(Duration.ofSeconds(60));
        } catch (WebClientResponseException ex) {
            String alt = alternateUrl(this.apiUrl);
            if (alt == null || (ex.getStatusCode().value() != 404 && ex.getStatusCode().value() != 401)) {
                throw ex;
            }
            WebClient altClient = WebClient.builder()
                    .baseUrl(alt)
                    .defaultHeader("Authorization", "Bearer " + this.apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
            response = altClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ModelResponse.class)
                    .block(Duration.ofSeconds(60));
        }
        if (response == null || response.getData() == null || response.getData().isEmpty()
                || response.getData().get(0).getB64_json() == null) {
            String url = (response != null && response.getData() != null && !response.getData().isEmpty())
                    ? response.getData().get(0).getUrl()
                    : null;
            if (url == null || url.isBlank()) {
                throw new RuntimeException("image_api_empty_response");
            }
            byte[] fromUrl;
            try {
                fromUrl = downloadImage(url);
            } catch (Exception e) {
                throw new RuntimeException("image_api_download_failed: " + e.getMessage(), e);
            }
            if (fromUrl == null || fromUrl.length == 0) {
                throw new RuntimeException("image_api_download_failed_empty");
            }
            return new GeneratedImage(fromUrl, "image/png", "generated.png");
        }
        String b64 = response.getData().get(0).getB64_json();
        byte[] bytes = Base64.getDecoder().decode(b64);
        return new GeneratedImage(bytes, "image/png", "generated.png");
    }

    private String alternateUrl(String url) {
        if (url == null)
            return null;
        if (url.contains("api.siliconflow.com")) {
            return url.replace("api.siliconflow.com", "api.siliconflow.cn");
        }
        if (url.contains("api.siliconflow.cn")) {
            return url.replace("api.siliconflow.cn", "api.siliconflow.com");
        }
        return null;
    }

    public static class GeneratedImage {
        private final byte[] bytes;
        private final String contentType;
        private final String suggestedName;

        public GeneratedImage(byte[] bytes, String contentType, String suggestedName) {
            this.bytes = bytes;
            this.contentType = contentType;
            this.suggestedName = suggestedName;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getContentType() {
            return contentType;
        }

        public String getSuggestedName() {
            return suggestedName;
        }
    }

    public static class ModelRequest {
        private String prompt;
        private String model;
        private Integer batch_size;
        private Integer num_inference_steps;
        private Double guidance_scale;
        private Double cfg;
        private String image_size;

        public ModelRequest(String prompt, String model) {
            this.prompt = prompt;
            this.model = model;
        }

        public ModelRequest(String prompt, String model, Map<String, Object> params) {
            this.prompt = prompt;
            this.model = model;
            if (params != null) {
                Object bs = params.get("batch_size");
                Object steps = params.get("steps");
                Object nis = params.get("num_inference_steps");
                Object gs = params.get("guidance_scale");
                Object cfg = params.get("cfg");
                Object size = params.get("size");
                Object imageSize = params.get("image_size");
                if (bs instanceof Number)
                    this.batch_size = ((Number) bs).intValue();
                if (nis instanceof Number)
                    this.num_inference_steps = ((Number) nis).intValue();
                else if (steps instanceof Number)
                    this.num_inference_steps = ((Number) steps).intValue();
                if (gs instanceof Number)
                    this.guidance_scale = ((Number) gs).doubleValue();
                if (cfg instanceof Number)
                    this.cfg = ((Number) cfg).doubleValue();
                if (imageSize instanceof String)
                    this.image_size = (String) imageSize;
                else if (size instanceof String)
                    this.image_size = (String) size;
            }
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Integer getBatch_size() {
            return batch_size;
        }

        public void setBatch_size(Integer batch_size) {
            this.batch_size = batch_size;
        }

        public Integer getNum_inference_steps() {
            return num_inference_steps;
        }

        public void setNum_inference_steps(Integer num_inference_steps) {
            this.num_inference_steps = num_inference_steps;
        }

        public Double getGuidance_scale() {
            return guidance_scale;
        }

        public void setGuidance_scale(Double guidance_scale) {
            this.guidance_scale = guidance_scale;
        }

        public Double getCfg() {
            return cfg;
        }

        public void setCfg(Double cfg) {
            this.cfg = cfg;
        }

        public String getImage_size() {
            return image_size;
        }

        public void setImage_size(String image_size) {
            this.image_size = image_size;
        }
    }

    private byte[] downloadImage(String url) {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)) // 20MB
                .build()
                .get()
                .uri(java.net.URI.create(url))
                .retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofSeconds(60));
    }

    public static class ModelResponse {
        private java.util.List<ImageData> data;

        public java.util.List<ImageData> getData() {
            return data;
        }

        public void setData(java.util.List<ImageData> data) {
            this.data = data;
        }
    }

    public static class ImageData {
        private String b64_json;
        private String url;

        public String getB64_json() {
            return b64_json;
        }

        public void setB64_json(String b64_json) {
            this.b64_json = b64_json;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
