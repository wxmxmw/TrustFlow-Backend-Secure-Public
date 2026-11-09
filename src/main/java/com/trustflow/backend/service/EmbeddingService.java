package com.trustflow.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public EmbeddingService(@Value("${app.embedding.api-url}") String apiUrl,
            @Value("${app.embedding.api-key}") String apiKey,
            @Value("${app.embedding.model}") String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                        .build())
                .build();
    }

    public float[] generateEmbedding(String text) {
        EmbeddingRequest request = new EmbeddingRequest(model, List.of(text));

        EmbeddingResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        if (response == null || response.getData().isEmpty()) {
            throw new RuntimeException("Failed to generate embedding");
        }

        return response.getData().get(0).getEmbedding();
    }

    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        EmbeddingRequest request = new EmbeddingRequest(model, texts);

        EmbeddingResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        if (response == null || response.getData().isEmpty()) {
            throw new RuntimeException("Failed to generate batch embeddings");
        }

        return response.getData().stream()
                .map(EmbeddingResponse.EmbeddingData::getEmbedding)
                .toList();
    }

    // DTO classes
    public static class EmbeddingRequest {
        private final String model;
        private final List<String> input;

        public EmbeddingRequest(String model, List<String> input) {
            this.model = model;
            this.input = input;
        }

        public String getModel() {
            return model;
        }

        public List<String> getInput() {
            return input;
        }
    }

    public static class EmbeddingResponse {
        private List<EmbeddingData> data;

        public List<EmbeddingData> getData() {
            return data;
        }

        public void setData(List<EmbeddingData> data) {
            this.data = data;
        }

        public static class EmbeddingData {
            private float[] embedding;

            public float[] getEmbedding() {
                return embedding;
            }

            public void setEmbedding(float[] embedding) {
                this.embedding = embedding;
            }
        }
    }
}