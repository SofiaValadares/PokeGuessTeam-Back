package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.game.GameFinishResponse;
import com.svc.pokeguessteam.dto.game.GameLocalFinishRequest;
import com.svc.pokeguessteam.dto.game.LocalMatchSetupRequest;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.LocalMatchService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Partida local: validação de equipas e registo de resultado no servidor; motor no cliente.
 */
@RestController
@RequestMapping("/api/game/local/match")
public class LocalMatchController {

    private final LocalMatchService localMatchService;
    private final CurrentUserService currentUserService;

    public LocalMatchController(LocalMatchService localMatchService, CurrentUserService currentUserService) {
        this.localMatchService = localMatchService;
        this.currentUserService = currentUserService;
    }

    @PutMapping("/setup")
    public ResponseEntity<Void> validateSetup(
            HttpSession session,
            @Valid @RequestBody LocalMatchSetupRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        localMatchService.validateSetupForClient(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/finish")
    public ResponseEntity<GameFinishResponse> finish(
            HttpSession session,
            @Valid @RequestBody GameLocalFinishRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(localMatchService.finishClientMatch(userId, request));
    }

    /** Abandona partida local ativa no servidor (motor no cliente). */
    @DeleteMapping
    public ResponseEntity<Void> abandon(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        localMatchService.abandonClientMatch(userId);
        return ResponseEntity.noContent().build();
    }
}
