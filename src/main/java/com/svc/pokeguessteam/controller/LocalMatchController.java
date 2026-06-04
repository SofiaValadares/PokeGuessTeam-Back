package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.LocalMatchActionResponse;
import com.svc.pokeguessteam.dto.game.LocalMatchStartRequest;
import com.svc.pokeguessteam.dto.game.LocalMatchStateDto;
import com.svc.pokeguessteam.dto.game.LocalMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.OpponentTeamKnowledgeResponse;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.LocalMatchService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Partida local (pass-and-play): dois jogadores no mesmo dispositivo, motor no servidor.
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

    @PostMapping
    public ResponseEntity<LocalMatchStateDto> start(
            HttpSession session,
            @Valid @RequestBody LocalMatchStartRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(localMatchService.startMatch(userId, request));
    }

    @GetMapping
    public ResponseEntity<LocalMatchStateDto> active(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(localMatchService.getActiveMatch(userId));
    }

    @GetMapping("/opponent-knowledge")
    public ResponseEntity<OpponentTeamKnowledgeResponse> opponentKnowledge(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(localMatchService.getOpponentKnowledge(userId));
    }

    @PutMapping("/team")
    public ResponseEntity<LocalMatchActionResponse> submitTeam(
            HttpSession session,
            @Valid @RequestBody LocalMatchTeamRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(localMatchService.submitTeam(userId, request));
    }

    @PostMapping("/guess")
    public ResponseEntity<LocalMatchActionResponse> guess(
            HttpSession session,
            @Valid @RequestBody BotMatchGuessRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(localMatchService.submitGuess(userId, request));
    }

    @PostMapping("/surrender")
    public ResponseEntity<LocalMatchActionResponse> surrender(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(localMatchService.surrender(userId));
    }
}
