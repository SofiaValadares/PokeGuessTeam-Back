package com.svc.pokeguessteam.realtime;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.dto.game.MatchJoinRequest;
import com.svc.pokeguessteam.dto.game.MatchWsGuessRequest;
import com.svc.pokeguessteam.service.FriendMatchService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Socket.io apenas para partidas online contra amigos. */
@Component
public class MatchSocketIoListener {

    private static final Logger log = LoggerFactory.getLogger(MatchSocketIoListener.class);

    public static final String SESSION_USER_ID = "USER_ID";

    private final SocketIOServer server;
    private final SocketSessionAuthService sessionAuthService;
    private final FriendMatchService friendMatchService;

    public MatchSocketIoListener(
            SocketIOServer server,
            SocketSessionAuthService sessionAuthService,
            FriendMatchService friendMatchService
    ) {
        this.server = server;
        this.sessionAuthService = sessionAuthService;
        this.friendMatchService = friendMatchService;
    }

    @PostConstruct
    public void registerListeners() {
        server.addConnectListener(this::onConnect);
        server.addDisconnectListener(client -> client.getAllRooms().forEach(client::leaveRoom));

        server.addEventListener("match:join", MatchJoinRequest.class, (client, data, ack) -> {
            String userId = requireUserId(client);
            if (userId == null || data == null) {
                return;
            }
            leaveMatchRooms(client);
            if ("friend".equalsIgnoreCase(data.mode())) {
                client.joinRoom(friendRoom(data.matchId(), userId));
                pushFriendMatchState(client, data.matchId(), userId);
            }
        });

        server.addEventListener("match:leave", MatchJoinRequest.class, (client, data, ack) -> {
            if (data == null) {
                leaveMatchRooms(client);
                return;
            }
            Object userId = client.get(SESSION_USER_ID);
            if ("friend".equalsIgnoreCase(data.mode()) && userId instanceof String id && !id.isBlank()) {
                client.leaveRoom(friendRoom(data.matchId(), id));
            }
        });

        server.addEventListener("match:friend:guess", MatchWsGuessRequest.class, (client, data, ack) -> {
            String userId = requireUserId(client);
            if (userId == null || data == null) {
                return;
            }
            friendMatchService.submitGuess(userId, new BotMatchGuessRequest(data.pokedexNumber()));
        });
    }

    private void onConnect(SocketIOClient client) {
        String cookie = client.getHandshakeData().getHttpHeaders().get("Cookie");
        sessionAuthService.resolveUserId(cookie).ifPresentOrElse(
                userId -> client.set(SESSION_USER_ID, userId),
                () -> client.disconnect()
        );
    }

    private String requireUserId(SocketIOClient client) {
        Object userId = client.get(SESSION_USER_ID);
        if (userId instanceof String id && !id.isBlank()) {
            return id;
        }
        client.disconnect();
        return null;
    }

    static String friendRoom(String matchId, String userId) {
        return "match:friend:" + matchId + ":user:" + userId;
    }

    private static void leaveMatchRooms(SocketIOClient client) {
        for (String room : client.getAllRooms()) {
            if (room.startsWith("match:")) {
                client.leaveRoom(room);
            }
        }
    }

    private void pushFriendMatchState(SocketIOClient client, String matchId, String userId) {
        try {
            FriendMatchStateDto state = friendMatchService.getStateForUser(matchId, userId);
            client.sendEvent("match:event", MatchRealtimeMessage.friendState(matchId, state));
        } catch (Exception ex) {
            log.debug("Sem estado inicial para match:join userId={} matchId={}: {}", userId, matchId, ex.getMessage());
        }
    }
}
