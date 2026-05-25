package com.learning.ai.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromptConfigService {

    private static final Logger log = LoggerFactory.getLogger(PromptConfigService.class);

    @Value("${ai.prompts.system:You are a helpful AI assistant for learning purposes.}")
    private String defaultSystemPrompt;

    @Value("${ai.prompts.max-history:10}")
    private int maxHistory;

    private final Map<String, String> promptTemplates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        promptTemplates.put("default", defaultSystemPrompt);
        promptTemplates.put("coding", "You are an expert coding assistant. Provide clear, concise code examples with explanations.");
        promptTemplates.put("debugging", "You are a debugging specialist. Help identify and fix issues in code with detailed explanations.");
        promptTemplates.put("explanation", "You are a teacher. Explain concepts in simple terms with examples.");
        promptTemplates.put("creative", "You are a creative writing assistant. Help generate creative content and ideas.");
        log.info("Prompt templates registered: {}", promptTemplates.keySet());
        log.info("Max conversation history: {} turns", maxHistory);
    }

    public String getSystemPrompt(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            log.debug("No template name provided, using default system prompt");
            return defaultSystemPrompt;
        }
        String resolved = promptTemplates.getOrDefault(templateName.toLowerCase(), defaultSystemPrompt);
        boolean found = promptTemplates.containsKey(templateName.toLowerCase());
        log.debug("Resolved template '{}': {} ({})", templateName, found ? "found" : "not found, using default",
                resolved.length() + " chars");
        return resolved;
    }

    public void setSystemPrompt(String templateName, String prompt) {
        promptTemplates.put(templateName.toLowerCase(), prompt);
        log.info("Updated prompt template: {}", templateName);
    }

    public Map<String, String> getAllPromptTemplates() {
        return Map.copyOf(promptTemplates);
    }

    public int getMaxHistory() {
        return maxHistory;
    }
}
