package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;

import java.time.LocalDateTime;
import java.util.List;

/** Partida vs IA: {@link MatchPlayerSide#HOST} = treinador; {@link MatchPlayerSide#OPPONENT} = bot. */
public record BotMatchStateDto(
        String matchId,
        MatchStatus status,
        MatchPlayerSide currentTurn,
        MatchPlayerSide startingPlayer,
        MatchPlayerSide finalResponseFor,
        List<Integer> hostTeam,
        List<Integer> hostHits,
        int hostCorrectGuesses,
        int opponentCorrectGuesses,
        List<OpponentSlotKnowledgeDto> opponentKnowledge,
        List<BotMatchGuessFeedbackDto> recentGuesses,
        MatchPlayerSide winner,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        GameHistoryEntryDto historyEntry
) {
    public static BotMatchStateDto from(
            ActiveMatchModel match,
            List<OpponentSlotKnowledgeDto> opponentKnowledge,
            List<BotMatchGuessFeedbackDto> recentGuesses,
            GameHistoryEntryDto historyEntry
    ) {
        return new BotMatchStateDto(
                match.getId(),
                match.getStatus(),
                match.getCurrentTurn(),
                match.getStartingPlayer(),
                match.getFinalResponseFor(),
                List.copyOf(match.getHostPlayer().getTeam()),
                List.copyOf(match.getHostPlayer().getHits()),
                match.getHostPlayer().getHits().size(),
                match.getOpponentPlayer().getHits().size(),
                opponentKnowledge,
                recentGuesses,
                match.getWinner(),
                match.getStartedAt(),
                match.getFinishedAt(),
                historyEntry
        );
    }
}
