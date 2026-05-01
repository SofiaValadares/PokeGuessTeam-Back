package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.CreateMatchRequest;
import com.svc.pokeguessteam.dto.GuessRequest;
import com.svc.pokeguessteam.dto.GuessResultResponse;
import com.svc.pokeguessteam.dto.MatchStateResponse;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.MatchService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final MatchService matchService;
    private final CurrentUserService currentUserService;

    public GameController(MatchService matchService, CurrentUserService currentUserService) {
        this.matchService = matchService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/matches/bot")
    public ResponseEntity<MatchStateResponse> createBotMatch(@RequestBody @Valid CreateMatchRequest request,
                                                             HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(matchService.createBotMatch(userId, request));
    }

    @PostMapping("/matches/{matchId}/guess")
    public ResponseEntity<GuessResultResponse> guess(@PathVariable String matchId,
                                                     @RequestBody @Valid GuessRequest request,
                                                     HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(matchService.makeGuess(matchId, userId, request.guessedPokemonId()));
    }

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<MatchStateResponse> state(@PathVariable String matchId,
                                                    HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(matchService.getState(matchId, userId));
    }
}
