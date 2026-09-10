package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.service.CompetitiveMatchService;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.logging.AppLogger;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/game/competitive")
public class CompetitiveMatchController {

    private static final AppLogger log = AppLogger.create(CompetitiveMatchController.class);

    private final CompetitiveMatchService competitiveMatchService;
    private final CurrentUserService currentUserService;

    public CompetitiveMatchController(
            CompetitiveMatchService competitiveMatchService,
            CurrentUserService currentUserService
    ) {
        this.competitiveMatchService = competitiveMatchService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/queue")
    public ResponseEntity<Map<String, Object>> enqueue(
            HttpSession session,
            @Valid @RequestBody BotMatchTeamRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        log.info("enqueue", "Entrada na fila competitiva userId={}", userId);
        Map<String, Object> raw = competitiveMatchService.enqueue(userId, request);
        return ResponseEntity.ok(normalizeForUser(userId, raw));
    }

    @GetMapping("/queue")
    public ResponseEntity<Map<String, Object>> status(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(competitiveMatchService.queueStatus(userId));
    }

    @DeleteMapping("/queue")
    public ResponseEntity<Void> leave(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        log.info("leave", "Saída da fila competitiva userId={}", userId);
        competitiveMatchService.leaveQueue(userId);
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeForUser(String userId, Map<String, Object> raw) {
        if (!"MATCHED".equals(raw.get("status"))) {
            return raw;
        }
        if (raw.containsKey("match") && raw.get("match") instanceof FriendMatchStateDto) {
            return raw;
        }
        String hostUserId = (String) raw.get("hostUserId");
        FriendMatchStateDto hostState = (FriendMatchStateDto) raw.get("matchHost");
        FriendMatchStateDto guestState = (FriendMatchStateDto) raw.get("matchGuest");
        FriendMatchStateDto mine = userId.equals(hostUserId) ? hostState : guestState;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "MATCHED");
        body.put("match", mine);
        return body;
    }
}
