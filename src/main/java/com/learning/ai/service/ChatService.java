package com.learning.ai.service;

import com.learning.ai.dto.ChatRequest;
import com.learning.ai.dto.ChatResponse;
import com.learning.ai.model.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final long STREAM_TIMEOUT_MS = 30_000L;

    private final ChatModel chatModel;
    private final ConversationHistoryService conversationHistoryService;
    private final PromptConfigService promptConfigService;

    public ChatService(ChatModel chatModel, ConversationHistoryService conversationHistoryService, PromptConfigService promptConfigService) {
        this.chatModel = chatModel;
        this.conversationHistoryService = conversationHistoryService;
        this.promptConfigService = promptConfigService;
    }

    public ChatResponse chat(ChatRequest request) {
        String conversationId = request.conversationId() != null ?
                request.conversationId() : UUID.randomUUID().toString();

        log.info("Chat request: conversationId={}, template='{}', messageLength={}",
                conversationId, request.promptTemplate(), request.message().length());

        List<Message> messages = buildMessages(request, conversationId);
        Prompt prompt = new Prompt(messages);

        long start = System.currentTimeMillis();
        var aiResponse = chatModel.call(prompt);
        String content = aiResponse.getResult().getOutput().getContent();
        long elapsed = System.currentTimeMillis() - start;

        log.info("Chat response received: conversationId={}, responseLength={}, elapsed={}ms",
                conversationId, content.length(), elapsed);

        conversationHistoryService.saveConversation(conversationId, request.message(), content);

        return new ChatResponse(
                conversationId,
                content,
                false,
                "openai",
                System.currentTimeMillis()
        );
    }

    public SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(() -> {
            log.warn("Stream timed out");
            sendStreamError(emitter, "Request timed out while waiting for AI response.");
            emitter.complete();
        });

        String conversationId = request.conversationId() != null ?
                request.conversationId() : UUID.randomUUID().toString();

        log.info("Stream request: conversationId={}, template='{}', messageLength={}",
                conversationId, request.promptTemplate(), request.message().length());

        List<Message> messages = buildMessages(request, conversationId);
        Prompt prompt = new Prompt(messages);

        StringBuilder fullResponse = new StringBuilder();
        long start = System.currentTimeMillis();

        chatModel.stream(prompt).subscribe(
                chunk -> {
                    try {
                        String text = chunk.getResult().getOutput().getContent();
                        if (text == null || text.isBlank()) {
                            return;
                        }
                        fullResponse.append(text);
                        log.debug("Stream chunk: conversationId={}, chunkLength={}", conversationId, text.length());
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(text + " ", MediaType.TEXT_PLAIN));
                    } catch (Exception e) {
                        log.error("Error streaming response: conversationId={}", conversationId, e);
                        sendStreamError(emitter, toUserErrorMessage(e));
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("Stream error: conversationId={}", conversationId, error);
                    sendStreamError(emitter, toUserErrorMessage(error));
                    emitter.completeWithError(error);
                },
                () -> {
                    try {
                        long elapsed = System.currentTimeMillis() - start;
                        log.info("Stream complete: conversationId={}, totalLength={}, elapsed={}ms",
                                conversationId, fullResponse.length(), elapsed);
                        conversationHistoryService.saveConversation(
                                conversationId,
                                request.message(),
                                fullResponse.toString()
                        );
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data("DONE"));
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("Error completing stream: conversationId={}", conversationId, e);
                        sendStreamError(emitter, toUserErrorMessage(e));
                        emitter.completeWithError(e);
                    }
                }
        );

        return emitter;
    }

    private List<Message> buildMessages(ChatRequest request, String conversationId) {
        List<Message> messages = new ArrayList<>();

        String systemPrompt = promptConfigService.getSystemPrompt(request.promptTemplate());
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        List<Conversation> history = conversationHistoryService.getConversationHistoryRaw(conversationId);
        int count = 0;
        for (Conversation conv : history) {
            if (count >= promptConfigService.getMaxHistory()) {
                break;
            }
            messages.add(new UserMessage(conv.getPrompt()));
            messages.add(new AssistantMessage(conv.getResponse()));
            count++;
        }

        log.debug("buildMessages: conversationId={}, historyTurns={}, totalMessages={}",
                conversationId, count, messages.size() + 1);

        messages.add(new UserMessage(request.message()));
        return messages;
    }

    private void sendStreamError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(message));
        } catch (IOException ioException) {
            log.debug("Could not send stream error event", ioException);
        }
    }

    private String toUserErrorMessage(Throwable error) {
        String raw = error != null ? error.getMessage() : null;
        if (raw == null || raw.isBlank()) {
            return "AI request failed. Please check server logs for details.";
        }
        return raw.replace('\n', ' ').trim();
    }
}
