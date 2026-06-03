package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.util.GameConstants;

import java.time.LocalDateTime;
import java.util.List;

public record LocalMatchStateDto(
        String matchId,
        String opponentName,
        MatchStatus status,
        MatchPlayerSide currentTurn,
        MatchPlayerSide startingPlayer,
        MatchPlayerSide finalResponseFor,
        boolean playerTeamReady,
        boolean opponentTeamReady,
        List<Integer> playerTeam,
        int playerCorrectGuesses,
        int opponentCorrectGuesses,
        List<OpponentKnowledgeSlotDto> opponentKnowledge,
        List<BotMatchGuessFeedbackDto> recentGuesses,
        MatchPlayerSide winner,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        GameHistoryEntryDto historyEntry
) {
    public static LocalMatchStateDto from(
            ActiveMatchModel match,
            List<OpponentKnowledgeSlotDto> opponentKnowledge,
            List<BotMatchGuessFeedbackDto> recentGuesses,
            GameHistoryEntryDto historyEntry
    ) {
        return new LocalMatchStateDto(
                match.getId(),
                match.getOpponentName(),
                match.getStatus(),
                match.getCurrentTurn(),
                match.getStartingPlayer(),
                match.getFinalResponseFor(),
                match.getUserPlayer().getTeam().size() >= GameConstants.TEAM_SIZE,
                match.getBotPlayer().getTeam().size() >= GameConstants.TEAM_SIZE,
                List.copyOf(match.getUserPlayer().getTeam()),
                match.getUserPlayer().getHits().size(),
                match.getBotPlayer().getHits().size(),
                opponentKnowledge,
                recentGuesses,
                match.getWinner(),
                match.getStartedAt(),
                match.getFinishedAt(),
                historyEntry
        );
    }
}
