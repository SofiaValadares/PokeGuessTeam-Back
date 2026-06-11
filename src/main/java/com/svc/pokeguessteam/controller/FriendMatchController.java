package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchActionResponse;
import com.svc.pokeguessteam.dto.game.FriendMatchJoinRequest;
import com.svc.pokeguessteam.dto.game.OpponentTeamKnowledgeResponse;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.FriendMatchService;
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
 * Partida vs amigo remoto: anfitrião gera código; convidado entra com o código.
 */
@RestController
@RequestMapping("/api/game/friend/match")
public class FriendMatchController {

    private final FriendMatchService friendMatchService;
    private final CurrentUserService currentUserService;

    public FriendMatchController(FriendMatchService friendMatchService, CurrentUserService currentUserService) {
        this.friendMatchService = friendMatchService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<FriendMatchStateDto> active(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return friendMatchService.findActiveMatch(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<FriendMatchStateDto> start(
            HttpSession session,
            @Valid @RequestBody BotMatchTeamRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(friendMatchService.startMatch(userId, request));
    }

    @PostMapping("/join")
    public ResponseEntity<FriendMatchStateDto> join(
            HttpSession session,
            @Valid @RequestBody FriendMatchJoinRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(friendMatchService.joinMatch(userId, request));
    }

    @GetMapping("/opponent-knowledge")
    public ResponseEntity<OpponentTeamKnowledgeResponse> opponentKnowledge(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(friendMatchService.getOpponentKnowledge(userId));
    }

    @PutMapping("/team")
    public ResponseEntity<FriendMatchActionResponse> submitTeam(
            HttpSession session,
            @Valid @RequestBody BotMatchTeamRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(friendMatchService.submitTeam(userId, request));
    }

    @PostMapping("/guess")
    public ResponseEntity<FriendMatchActionResponse> guess(
            HttpSession session,
            @Valid @RequestBody BotMatchGuessRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(friendMatchService.submitGuess(userId, request));
    }

    @PostMapping("/surrender")
    public ResponseEntity<FriendMatchActionResponse> surrender(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(friendMatchService.surrender(userId));
    }

    @DeleteMapping
    public ResponseEntity<Void> leave(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        friendMatchService.leaveMatch(userId);
        return ResponseEntity.noContent().build();
    }
}
