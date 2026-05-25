package com.learning.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversationResponse(
        String conversationId,
        String prompt,
        String response,
        long timestamp
) {}
