package com.learning.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Message cannot be blank")
        @Size(max = 4000, message = "Message cannot exceed 4000 characters")
        String message,

        String conversationId,

        boolean stream,

        String promptTemplate
) {}
