package com.learning.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String conversationId,
        String response,
        boolean isStreaming,
        String model,
        long timestamp
) {}
