package com.procuresense.backend.controller;

import com.procuresense.backend.model.AssistantChatRequest;
import com.procuresense.backend.model.AssistantChatResponse;
import com.procuresense.backend.service.AssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(
            @RequestHeader("X-Org-Id") @NotBlank String orgId,
            @RequestHeader("X-Role") @NotBlank String role,
            @RequestHeader(value = "X-Use-Cached-AI", defaultValue = "false") boolean useCachedAi,
            @Valid @RequestBody AssistantChatRequest request) {
        return ResponseEntity.ok(assistantService.handleChat(orgId, request, useCachedAi));
    }
}
