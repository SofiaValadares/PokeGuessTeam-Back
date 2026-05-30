package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.game.BotMatchActionResponse;
import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.BotMatchStateDto;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.service.BotMatchService;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Partida vs bot com lógica de palpites no servidor (modo mais simples).
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

    @PostMapping
    public ResponseEntity<BotMatchStateDto> start(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(botMatchService.startMatch(userId));
    }

    @GetMapping
    public ResponseEntity<BotMatchStateDto> active(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(botMatchService.getActiveMatch(userId));
    }

    @PutMapping("/team")
    public ResponseEntity<BotMatchActionResponse> submitTeam(
            HttpSession session,
            @Valid @RequestBody BotMatchTeamRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(botMatchService.submitTeam(userId, request));
    }

    @PostMapping("/guess")
    public ResponseEntity<BotMatchActionResponse> guess(
            HttpSession session,
            @Valid @RequestBody BotMatchGuessRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(botMatchService.submitGuess(userId, request));
    }

    @PostMapping("/surrender")
    public ResponseEntity<BotMatchActionResponse> surrender(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(botMatchService.surrender(userId));
    }

    @DeleteMapping
    public ResponseEntity<Void> abandon(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        botMatchService.abandonMatch(userId);
        return ResponseEntity.noContent().build();
    }
}
