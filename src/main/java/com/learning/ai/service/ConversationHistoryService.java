package com.learning.ai.service;

import com.learning.ai.dto.ConversationResponse;
import com.learning.ai.model.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationHistoryService.class);

    private final Map<String, List<Conversation>> conversationStore = new ConcurrentHashMap<>();

    public void saveConversation(String conversationId, String prompt, String response) {
        Conversation conversation = new Conversation(conversationId, prompt, response);
        conversationStore.computeIfAbsent(conversationId, key -> Collections.synchronizedList(new ArrayList<>()))
                .add(conversation);
        log.info("Saved conversation: {}", conversationId);
    }

    public List<ConversationResponse> getConversationHistory(String conversationId) {
        List<Conversation> snapshot = new ArrayList<>(conversationStore.getOrDefault(conversationId, Collections.emptyList()));
        List<ConversationResponse> result = snapshot
                .stream()
                .map(conv -> new ConversationResponse(
                        conv.getConversationId(),
                        conv.getPrompt(),
                        conv.getResponse(),
                        conv.getTimestamp()
                ))
                .toList();
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

}
