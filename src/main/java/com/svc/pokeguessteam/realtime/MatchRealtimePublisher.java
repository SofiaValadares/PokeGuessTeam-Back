package com.svc.pokeguessteam.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class MatchRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public MatchRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishBot(String matchId, MatchRealtimeMessage message) {
        messagingTemplate.convertAndSend("/topic/match/bot/" + matchId, message);
    }

    public void publishFriendToUser(String matchId, String userId, MatchRealtimeMessage message) {
        messagingTemplate.convertAndSend("/topic/match/friend/" + matchId + "/user/" + userId, message);
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
