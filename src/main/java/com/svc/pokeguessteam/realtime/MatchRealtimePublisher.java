package com.svc.pokeguessteam.realtime;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.stereotype.Component;

@Component
public class MatchRealtimePublisher {

    private final SocketIOServer server;

    public MatchRealtimePublisher(SocketIOServer server) {
        this.server = server;
    }

    public void publishFriendToUser(String matchId, String userId, MatchRealtimeMessage message) {
        server.getRoomOperations(MatchSocketIoListener.friendRoom(matchId, userId))
                .sendEvent("match:event", message);
    }

    public void publishFriendToBoth(
            String matchId,
            String hostUserId,
            String guestUserId,
            MatchRealtimeMessage hostView,
            MatchRealtimeMessage guestView
    ) {
        if (hostUserId != null) {
            publishFriendToUser(matchId, hostUserId, hostView);
        }
        if (guestUserId != null) {
            publishFriendToUser(matchId, guestUserId, guestView);
        }
    }
}
