package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.FriendMatchStore;
import com.svc.pokeguessteam.service.PusherRealtimeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pusher")
public class PusherController {

    private final PusherRealtimeService pusherRealtimeService;
    private final CurrentUserService currentUserService;
    private final FriendMatchStore friendMatchStore;

    public PusherController(
            PusherRealtimeService pusherRealtimeService,
            CurrentUserService currentUserService,
            FriendMatchStore friendMatchStore
    ) {
        this.pusherRealtimeService = pusherRealtimeService;
        this.currentUserService = currentUserService;
        this.friendMatchStore = friendMatchStore;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config(HttpSession session) {
        currentUserService.requireUserId(session);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", pusherRealtimeService.isEnabled());
        body.put("key", pusherRealtimeService.isEnabled() ? pusherRealtimeService.getPublicKey() : null);
        body.put("cluster", pusherRealtimeService.isEnabled() ? pusherRealtimeService.getCluster() : null);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> auth(
            HttpSession session,
            @RequestBody Map<String, String> body
    ) {
        String userId = currentUserService.requireUserId(session);
        if (!pusherRealtimeService.isEnabled()) {
            throw new ApiBusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCodes.VALIDATION_FAILED,
                    MessageKeys.VALIDATION_SUMMARY
            );
        }
        String socketId = body.get("socket_id");
        String channelName = body.get("channel_name");
        if (socketId == null || channelName == null) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.VALIDATION_FAILED,
                    MessageKeys.VALIDATION_SUMMARY
            );
        }
        if (!canSubscribe(userId, channelName)) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.ADMIN_FORBIDDEN,
                    MessageKeys.ADMIN_FORBIDDEN
            );
        }
        return ResponseEntity.ok(pusherRealtimeService.authenticate(socketId, channelName));
    }

    private boolean canSubscribe(String userId, String channelName) {
        if (channelName.equals(PusherRealtimeService.userChannel(userId))) {
            return true;
        }
        String matchPrefix = "private-match-";
        if (!channelName.startsWith(matchPrefix) || channelName.length() <= matchPrefix.length()) {
            return false;
        }
        String matchId = channelName.substring(matchPrefix.length());
        return friendMatchStore.findById(matchId)
                .filter(match -> isParticipant(match, userId))
                .isPresent();
    }

    private static boolean isParticipant(ActiveMatchModel match, String userId) {
        if (match.getProfile() != null
                && match.getProfile().getUser() != null
                && userId.equals(match.getProfile().getUser().getIdUser())) {
            return true;
        }
        return match.getGuestProfile() != null
                && match.getGuestProfile().getUser() != null
                && userId.equals(match.getGuestProfile().getUser().getIdUser());
    }
}
