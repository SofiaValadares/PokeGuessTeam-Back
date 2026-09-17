package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.game.BotMatchGuessCheckResponse;
import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.BotMatchSetupResponse;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.GameBotFinishRequest;
import com.svc.pokeguessteam.dto.game.GameFinishResponse;
import com.svc.pokeguessteam.service.BotMatchService;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Partida vs bot: validação de equipa e palpite no servidor; motor de turno no cliente.
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

    @PostMapping("/guess")
    public ResponseEntity<BotMatchGuessCheckResponse> guess(
            HttpSession session,
            @Valid @RequestBody BotMatchGuessRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(botMatchService.evaluateGuess(userId, request));
    }

    @PostMapping("/finish")
    public ResponseEntity<GameFinishResponse> finish(
            HttpSession session,
            @Valid @RequestBody GameBotFinishRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(botMatchService.finishClientMatch(userId, request));
    }
}
