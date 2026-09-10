package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.game.GameHistoryPageResponse;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.GameHistoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameHistoryController {

    private final GameHistoryService gameHistoryService;
    private final CurrentUserService currentUserService;

    public GameHistoryController(GameHistoryService gameHistoryService, CurrentUserService currentUserService) {
        this.gameHistoryService = gameHistoryService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/history")
    public ResponseEntity<GameHistoryPageResponse> history(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + GameHistoryService.DEFAULT_PAGE_SIZE) int size
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(gameHistoryService.listHistory(userId, page, size));
    }

    @DeleteMapping("/history/{gameId}")
    public ResponseEntity<Void> deleteHistory(HttpSession session, @PathVariable String gameId) {
        String userId = currentUserService.requireUserId(session);
        gameHistoryService.deleteHistory(userId, gameId);
        return ResponseEntity.noContent().build();
    }
}
