package com.svc.pokeguessteam.dto.game;

public record FriendMatchParticipantDto(
        String userId,
        String username,
        boolean teamReady,
        int timeoutPenalties
) {
}
