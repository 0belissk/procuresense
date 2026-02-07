package com.procuresense.backend.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record AssistantChatRequest(
        @NotBlank String message,
        @Valid AssistantContext context) {

    public record AssistantContext(String selectedSku,
                                   String orgType,
                                   String projectType) {
    }
}
