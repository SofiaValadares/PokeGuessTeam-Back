package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.game.HistoryGamePlayerModel;
import com.svc.pokeguessteam.model.user.ProfileModel;

import java.util.List;

public record GameHistoryPlayerDto(
        int slot,
        String profileId,
        String username,
        int correctGuesses,
        GameResults result,
        List<GameHistoryOpponentSlotDto> opponentTeam
) {
    public static GameHistoryPlayerDto from(HistoryGamePlayerModel player) {
        ProfileModel profile = player.getProfile();
        String profileId = profile != null ? profile.getId() : null;
        String username = null;
        if (profile != null && profile.getUser() != null) {
            username = profile.getUser().getUsername();
        }
        return new GameHistoryPlayerDto(
                player.getSlot(),
                profileId,
                username,
                player.getCorrectGuesses(),
                player.getResult(),
                player.getOpponentTeamSnapshot()
        );
    }
}
