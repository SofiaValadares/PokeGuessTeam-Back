package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.game.GameBotFinishRequest;
import com.svc.pokeguessteam.dto.game.GameFriendFinishRequest;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.GameHistoryPageResponse;
import com.svc.pokeguessteam.dto.game.GameLocalFinishRequest;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.GameHistoryService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/local")
    public ResponseEntity<GameHistoryEntryDto> finishLocal(
            HttpSession session,
            @Valid @RequestBody GameLocalFinishRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameHistoryService.saveLocalGame(userId, request));
    }

    @PostMapping("/bot")
    public ResponseEntity<GameHistoryEntryDto> finishBot(
            HttpSession session,
            @Valid @RequestBody GameBotFinishRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameHistoryService.saveBotGame(userId, request));
    }

    @PostMapping("/friend")
    public ResponseEntity<GameHistoryEntryDto> finishFriend(
            HttpSession session,
            @Valid @RequestBody GameFriendFinishRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameHistoryService.saveFriendGame(userId, request));
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
}
