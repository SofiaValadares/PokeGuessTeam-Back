package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.game.BotMatchSetupResponse;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.GameBotFinishRequest;
import com.svc.pokeguessteam.dto.game.GameFinishResponse;
import com.svc.pokeguessteam.service.BotMatchService;
import com.svc.pokeguessteam.service.CurrentUserService;
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
 * Partida vs bot: validação de equipa e registo de resultado no servidor; motor no cliente.
 */
@RestController
@RequestMapping("/api/game/bot/match")
public class BotMatchController {

    private final BotMatchService botMatchService;
    private final CurrentUserService currentUserService;

    public BotMatchController(BotMatchService botMatchService, CurrentUserService currentUserService) {
        this.botMatchService = botMatchService;
        this.currentUserService = currentUserService;
    }

    @PutMapping("/team")
    public ResponseEntity<BotMatchSetupResponse> validateTeam(
            HttpSession session,
            @Valid @RequestBody BotMatchTeamRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(botMatchService.validateTeamForClient(userId, request));
    }

    @PostMapping("/finish")
    public ResponseEntity<GameFinishResponse> finish(
            HttpSession session,
            @Valid @RequestBody GameBotFinishRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(botMatchService.finishClientMatch(userId, request));
    }

    /** Abandona partida bot ativa no servidor (motor no cliente). */
    @DeleteMapping
    public ResponseEntity<Void> abandon(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        botMatchService.abandonClientMatch(userId);
        return ResponseEntity.noContent().build();
    }
}
