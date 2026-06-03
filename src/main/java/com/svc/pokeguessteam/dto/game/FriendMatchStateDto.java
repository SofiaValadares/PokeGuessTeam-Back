package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.util.GameConstants;

import java.time.LocalDateTime;
import java.util.List;

public record FriendMatchStateDto(
        String matchId,
        String joinCode,
        MatchStatus status,
        MatchPlayerSide yourSide,
        boolean yourTurn,
        MatchPlayerSide currentTurn,
        MatchPlayerSide startingPlayer,
        MatchPlayerSide finalResponseFor,
        List<Integer> yourTeam,
        List<Integer> yourHits,
        int yourCorrectGuesses,
        int opponentCorrectGuesses,
        FriendMatchParticipantDto host,
        FriendMatchParticipantDto guest,
        List<OpponentKnowledgeSlotDto> opponentKnowledge,
        List<BotMatchGuessFeedbackDto> recentGuesses,
        MatchPlayerSide winner,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        GameHistoryEntryDto historyEntry
) {
    public static FriendMatchStateDto from(
            ActiveMatchModel match,
            MatchPlayerSide viewerSide,
            List<OpponentKnowledgeSlotDto> opponentKnowledge,
            List<BotMatchGuessFeedbackDto> recentGuesses,
            GameHistoryEntryDto historyEntry
    ) {
        ActiveMatchPlayerModel yours = viewerSide == MatchPlayerSide.USER
                ? match.getUserPlayer()
                : match.getBotPlayer();
        ActiveMatchPlayerModel opponent = viewerSide == MatchPlayerSide.USER
                ? match.getBotPlayer()
                : match.getUserPlayer();
        return new FriendMatchStateDto(
                match.getId(),
                match.getJoinCode(),
                match.getStatus(),
                viewerSide,
                match.getCurrentTurn() == viewerSide,
                match.getCurrentTurn(),
                match.getStartingPlayer(),
                match.getFinalResponseFor(),
                List.copyOf(yours.getTeam()),
                List.copyOf(yours.getHits()),
                yours.getHits().size(),
                opponent.getHits().size(),
                participant(match.getProfile(), match.getUserPlayer().getTeam().size()),
                guestParticipant(match),
                opponentKnowledge,
                recentGuesses,
                match.getWinner(),
                match.getStartedAt(),
                match.getFinishedAt(),
                historyEntry
        );
    }

    private static FriendMatchParticipantDto participant(
            ProfileModel profile,
            int teamSize
    ) {
        return new FriendMatchParticipantDto(
                profile.getUser().getIdUser(),
                profile.getUser().getUsername(),
                teamSize >= GameConstants.TEAM_SIZE
        );
    }

    private static FriendMatchParticipantDto guestParticipant(ActiveMatchModel match) {
        ProfileModel guest = match.getGuestProfile();
        if (guest == null) {
            return new FriendMatchParticipantDto(null, null, false);
        }
        return participant(guest, match.getBotPlayer().getTeam().size());
    }
}
