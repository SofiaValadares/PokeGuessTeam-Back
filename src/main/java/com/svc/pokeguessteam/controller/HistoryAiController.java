package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.ai.HistoryChatRequest;
import com.svc.pokeguessteam.dto.ai.HistoryChatResponse;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.HistoryAssistantService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/history")
public class HistoryAiController {

    private final HistoryAssistantService historyAssistantService;
    private final CurrentUserService currentUserService;

    public HistoryAiController(HistoryAssistantService historyAssistantService, CurrentUserService currentUserService) {
        this.historyAssistantService = historyAssistantService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/chat")
    public ResponseEntity<HistoryChatResponse> chat(
            HttpSession session,
            @Valid @RequestBody HistoryChatRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(historyAssistantService.chat(userId, request.message()));
    }
}