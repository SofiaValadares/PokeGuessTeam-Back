package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;

import java.time.LocalDateTime;
import java.util.List;

public record BotMatchStateDto(
        String matchId,
        MatchStatus status,
        MatchPlayerSide currentTurn,
        MatchPlayerSide startingPlayer,
        MatchPlayerSide finalResponseFor,
        List<Integer> userTeam,
        List<Integer> userHits,
        int userCorrectGuesses,
        int opponentCorrectGuesses,
        List<OpponentKnowledgeSlotDto> opponentKnowledge,
        List<BotMatchGuessFeedbackDto> recentGuesses,
        MatchPlayerSide winner,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        GameHistoryEntryDto historyEntry
) {
    public static BotMatchStateDto from(
            ActiveMatchModel match,
            List<OpponentKnowledgeSlotDto> opponentKnowledge,
            List<BotMatchGuessFeedbackDto> recentGuesses,
            GameHistoryEntryDto historyEntry
    ) {
        return new BotMatchStateDto(
                match.getId(),
                match.getStatus(),
                match.getCurrentTurn(),
                match.getStartingPlayer(),
                match.getFinalResponseFor(),
                List.copyOf(match.getUserPlayer().getTeam()),
                List.copyOf(match.getUserPlayer().getHits()),
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
