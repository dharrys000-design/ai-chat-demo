package com.learning.ai.service;

import com.learning.ai.dto.ConversationResponse;
import com.learning.ai.model.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConversationHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationHistoryService.class);

    private final Map<String, List<Conversation>> conversationStore = new ConcurrentHashMap<>();

    public void saveConversation(String conversationId, String prompt, String response) {
        Conversation conversation = new Conversation(conversationId, prompt, response);
        conversationStore.computeIfAbsent(conversationId, k -> new ArrayList<>()).add(conversation);
        log.info("Saved conversation: {}", conversationId);
    }

    public List<ConversationResponse> getConversationHistory(String conversationId) {
        List<ConversationResponse> result = conversationStore.getOrDefault(conversationId, Collections.emptyList())
                .stream()
                .map(conv -> new ConversationResponse(
                        conv.getConversationId(),
                        conv.getPrompt(),
                        conv.getResponse(),
                        conv.getTimestamp()
                ))
                .collect(Collectors.toList());
        log.debug("Fetched conversation history: conversationId={}, turns={}", conversationId, result.size());
        return result;
    }

    public List<Conversation> getConversationHistoryRaw(String conversationId) {
        List<Conversation> result = new ArrayList<>(conversationStore.getOrDefault(conversationId, Collections.emptyList()));
        log.debug("Fetched raw conversation history: conversationId={}, turns={}", conversationId, result.size());
        return result;
    }

    public List<String> getAllConversationIds() {
        List<String> ids = new ArrayList<>(conversationStore.keySet());
        log.debug("Active conversations: {}", ids.size());
        return ids;
    }

    public void clearConversation(String conversationId) {
        conversationStore.remove(conversationId);
        log.info("Cleared conversation: {}", conversationId);
    }

    public void clearAllConversations() {
        conversationStore.clear();
        log.info("Cleared all conversations");
    }
}
