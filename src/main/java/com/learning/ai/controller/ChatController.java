package com.learning.ai.controller;

import com.learning.ai.dto.ChatRequest;
import com.learning.ai.dto.ChatResponse;
import com.learning.ai.dto.ConversationResponse;
import com.learning.ai.service.ChatService;
import com.learning.ai.service.ConversationHistoryService;
import com.learning.ai.service.PromptConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final ConversationHistoryService conversationHistoryService;
    private final PromptConfigService promptConfigService;

    public ChatController(ChatService chatService, ConversationHistoryService conversationHistoryService, PromptConfigService promptConfigService) {
        this.chatService = chatService;
        this.conversationHistoryService = conversationHistoryService;
        this.promptConfigService = promptConfigService;
    }

    @PostMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        log.debug("POST /api/chat/message: conversationId={}, template='{}', stream={}",
                request.conversationId(), request.promptTemplate(), request.stream());
        if (request.stream()) {
            log.warn("Client sent stream=true to /message endpoint, returning 501");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(new ChatResponse(null, "Use /stream endpoint for streaming", false, null, System.currentTimeMillis()));
        }
        return ResponseEntity.ok(chatService.chat(request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@Valid @RequestBody ChatRequest request) {
        log.debug("POST /api/chat/stream: conversationId={}, template='{}'",
                request.conversationId(), request.promptTemplate());
        return chatService.streamChat(request);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<String>> getAllConversations() {
        return ResponseEntity.ok(conversationHistoryService.getAllConversationIds());
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<ConversationResponse>> getConversation(@PathVariable String conversationId) {
        return ResponseEntity.ok(conversationHistoryService.getConversationHistory(conversationId));
    }

    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String conversationId) {
        conversationHistoryService.clearConversation(conversationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/prompts")
    public ResponseEntity<Map<String, String>> getPromptTemplates() {
        return ResponseEntity.ok(promptConfigService.getAllPromptTemplates());
    }

    @PostMapping("/prompts/{templateName}")
    public ResponseEntity<String> setPromptTemplate(
            @PathVariable String templateName,
            @RequestBody Map<String, String> request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("Request body cannot be null");
        }

        String prompt = request.get("prompt");
        if (prompt != null && !prompt.isBlank()) {
            promptConfigService.setSystemPrompt(templateName, prompt);
            return ResponseEntity.ok("Prompt template updated: " + templateName);
        }
        return ResponseEntity.badRequest().body("Prompt cannot be null or blank");
    }
}
