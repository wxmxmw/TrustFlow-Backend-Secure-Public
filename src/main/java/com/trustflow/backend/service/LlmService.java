package com.trustflow.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class LlmService {

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public LlmService(@Value("${app.llm.api-url}") String apiUrl,
            @Value("${app.llm.api-key}") String apiKey,
            @Value("${app.llm.model:zai-org/GLM-4.5}") String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                        .build())
                .build();
    }

    public String generateCompletion(String prompt) {
        return generateCompletion(List.of(new Message("user", prompt)));
    }

    public String generateCompletion(List<Message> messages) {
        LlmRequest request = new LlmRequest(model, messages);

        LlmResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LlmResponse.class)
                .block();

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("LLM API returned empty response");
        }

        return response.getChoices().get(0).getMessage().getContent();
    }

    public static class LlmRequest {
        private String model;
        private List<Message> messages;

        public LlmRequest(String model, String prompt) {
            this.model = model;
            this.messages = List.of(new Message("user", prompt));
        }

        public LlmRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class LlmResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }

        public static class Choice {
            private Message message;

            public Message getMessage() {
                return message;
            }

            public void setMessage(Message message) {
                this.message = message;
            }
        }
    }
}
