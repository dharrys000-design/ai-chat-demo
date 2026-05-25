package com.learning.ai.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:unknown}")
    private String model;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @PostConstruct
    public void logStartupConfig() {
        boolean keySet = apiKey != null && !apiKey.isBlank();
        String keyStatus = keySet ? "set" : "MISSING - requests will fail";
        log.info("=== AI Learning App Configuration ===");
        log.info("  OpenAI base URL : {}", baseUrl);
        log.info("  Model           : {}", model);
        log.info("  API key         : {}", keyStatus);
        log.info("=====================================");
        if (!keySet) {
            log.warn("OPENAI_API_KEY is not set. Set it via environment variable before sending requests.");
        }
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are a helpful AI assistant for learning purposes.")
                .build();
    }
}
