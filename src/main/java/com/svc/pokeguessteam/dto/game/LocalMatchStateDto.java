package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.util.GameConstants;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Estado da partida local (pass-and-play): dois jogadores no mesmo dispositivo.
 * {@link MatchPlayerSide#HOST} = conta logada; {@link MatchPlayerSide#OPPONENT} = 2.º jogador ({@code localOpponentName}).
 */
public record LocalMatchStateDto(
        String matchId,
        String hostDisplayName,
        String localOpponentName,
        MatchStatus status,
        MatchPlayerSide currentTurn,
        MatchPlayerSide startingPlayer,
        MatchPlayerSide finalResponseFor,
        boolean hostTeamReady,
        boolean opponentTeamReady,
        List<Integer> hostTeam,
        List<Integer> opponentTeam,
        List<Integer> hostHits,
        List<Integer> opponentHits,
        int hostCorrectGuesses,
        int opponentCorrectGuesses,
        List<OpponentSlotKnowledgeDto> opponentKnowledge,
        List<BotMatchGuessFeedbackDto> recentGuesses,
        MatchPlayerSide winner,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        GameHistoryEntryDto historyEntry
) {
    public static LocalMatchStateDto from(
            ActiveMatchModel match,
            List<OpponentSlotKnowledgeDto> opponentKnowledge,
            List<BotMatchGuessFeedbackDto> recentGuesses,
            GameHistoryEntryDto historyEntry
    ) {
        String hostName = match.getProfile() != null && match.getProfile().getUser() != null
                ? match.getProfile().getUser().getUsername()
                : null;
        return new LocalMatchStateDto(
                match.getId(),
                hostName,
                match.getOpponentName(),
                match.getStatus(),
                match.getCurrentTurn(),
                match.getStartingPlayer(),
                match.getFinalResponseFor(),
                match.getHostPlayer().getTeam().size() >= GameConstants.TEAM_SIZE,
                match.getOpponentPlayer().getTeam().size() >= GameConstants.TEAM_SIZE,
                List.copyOf(match.getHostPlayer().getTeam()),
                List.copyOf(match.getOpponentPlayer().getTeam()),
                sortedDexList(match.getHostPlayer().getHits()),
                sortedDexList(match.getOpponentPlayer().getHits()),
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

    private static List<Integer> sortedDexList(Set<Integer> dexNumbers) {
        return dexNumbers.stream().sorted().toList();
    }
}
