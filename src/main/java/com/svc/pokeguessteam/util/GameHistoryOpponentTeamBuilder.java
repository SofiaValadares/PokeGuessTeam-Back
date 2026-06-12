package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.dto.game.GameHistoryOpponentSlotDto;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GameHistoryOpponentTeamBuilder {

    private GameHistoryOpponentTeamBuilder() {
    }

    public static List<GameHistoryOpponentSlotDto> fromActiveMatch(
            ActiveMatchModel match,
            MatchPlayerSide viewerSide
    ) {
        ActiveMatchPlayerModel viewer = match.getPlayer(viewerSide);
        ActiveMatchPlayerModel opponent = match.getOpponent(viewerSide);
        Set<Integer> hits = new HashSet<>(viewer.getHits());
        List<Integer> team = opponent.getTeam();

        List<GameHistoryOpponentSlotDto> slots = new ArrayList<>(GameConstants.TEAM_SIZE);
        for (int i = 0; i < team.size() && i < GameConstants.TEAM_SIZE; i++) {
            int dex = team.get(i);
            slots.add(new GameHistoryOpponentSlotDto(i + 1, dex, hits.contains(dex)));
        }
        return List.copyOf(slots);
    }

    public static List<GameHistoryOpponentSlotDto> fromClientTeam(
            List<Integer> opponentTeam,
            List<Integer> viewerHits
    ) {
        if (opponentTeam == null || opponentTeam.isEmpty()) {
            return List.of();
        }
        Set<Integer> hits = viewerHits != null ? new HashSet<>(viewerHits) : Set.of();
        List<GameHistoryOpponentSlotDto> slots = new ArrayList<>(GameConstants.TEAM_SIZE);
        for (int i = 0; i < opponentTeam.size() && i < GameConstants.TEAM_SIZE; i++) {
            int dex = opponentTeam.get(i);
            slots.add(new GameHistoryOpponentSlotDto(i + 1, dex, hits.contains(dex)));
        }
        return List.copyOf(slots);
    }
}
