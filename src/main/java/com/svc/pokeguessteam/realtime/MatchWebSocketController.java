package com.svc.pokeguessteam.realtime;

import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.MatchWsGuessRequest;
import com.svc.pokeguessteam.service.BotMatchService;
import com.svc.pokeguessteam.service.FriendMatchService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Palpites em tempo real (STOMP). Estado e palpites do bot/adversário chegam nos tópicos {@code /topic/match/...}.
 */
@Controller
public class MatchWebSocketController {

    private final BotMatchService botMatchService;
    private final FriendMatchService friendMatchService;

    public MatchWebSocketController(
            BotMatchService botMatchService,
            FriendMatchService friendMatchService
    ) {
        this.botMatchService = botMatchService;
        this.friendMatchService = friendMatchService;
    }

    @MessageMapping("/match/bot/guess")
    public void botGuess(@Payload @Valid MatchWsGuessRequest request, Principal principal) {
        String userId = requireUserId(principal);
        botMatchService.submitGuess(userId, new BotMatchGuessRequest(request.pokedexNumber()));
    }

    @MessageMapping("/match/friend/guess")
    public void friendGuess(@Payload @Valid MatchWsGuessRequest request, Principal principal) {
        String userId = requireUserId(principal);
        friendMatchService.submitGuess(userId, new BotMatchGuessRequest(request.pokedexNumber()));
    }

    private static String requireUserId(Principal principal) {
        String userId = StompAuthChannelInterceptor.resolveUserId(principal);
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Sessão WebSocket sem utilizador autenticado.");
        }
        return userId;
    }
}
