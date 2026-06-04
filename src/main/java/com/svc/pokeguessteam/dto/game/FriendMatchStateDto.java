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
        List<OpponentSlotKnowledgeDto> opponentKnowledge,
        List<BotMatchGuessFeedbackDto> recentGuesses,
        MatchPlayerSide winner,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime turnDeadlineAt,
        int yourTimeoutPenalties,
        boolean opponentReplacedByBot,
        GameHistoryEntryDto historyEntry
) {
    public static FriendMatchStateDto from(
            ActiveMatchModel match,
            MatchPlayerSide viewerSide,
            List<OpponentSlotKnowledgeDto> opponentKnowledge,
            List<BotMatchGuessFeedbackDto> recentGuesses,
            GameHistoryEntryDto historyEntry
    ) {
        ActiveMatchPlayerModel yours = viewerSide == MatchPlayerSide.HOST
                ? match.getHostPlayer()
                : match.getOpponentPlayer();
        ActiveMatchPlayerModel opponent = viewerSide == MatchPlayerSide.HOST
                ? match.getOpponentPlayer()
                : match.getHostPlayer();
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
                participant(match.getProfile(), match.getHostPlayer().getTeam().size(), match.getHostPlayer().getTurnTimeoutPenalties()),
                guestParticipant(match),
                opponentKnowledge,
                recentGuesses,
                match.getWinner(),
                match.getStartedAt(),
                match.getFinishedAt(),
                match.getTurnDeadlineAt(),
                yours.getTurnTimeoutPenalties(),
                match.getBotReplacementSide() != null && match.getBotReplacementSide() != viewerSide,
                historyEntry
        );
    }

    private static FriendMatchParticipantDto participant(
            ProfileModel profile,
            int teamSize,
            int timeoutPenalties
    ) {
        return new FriendMatchParticipantDto(
                profile.getUser().getIdUser(),
                profile.getUser().getUsername(),
                teamSize >= GameConstants.TEAM_SIZE,
                timeoutPenalties
        );
    }

    private static FriendMatchParticipantDto guestParticipant(ActiveMatchModel match) {
        ProfileModel guest = match.getGuestProfile();
        if (guest == null) {
            return new FriendMatchParticipantDto(null, null, false, 0);
        }
        return participant(guest, match.getOpponentPlayer().getTeam().size(), match.getOpponentPlayer().getTurnTimeoutPenalties());
    }
}
