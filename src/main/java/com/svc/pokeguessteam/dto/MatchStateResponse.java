package com.svc.pokeguessteam.dto;

import java.util.List;

public record MatchStateResponse(
        String matchId,
        String status,
        String currentTurnSide,
        Integer playerScore,
        Integer opponentScore,
        List<MatchSlotResponse> playerTeam,
        List<MatchSlotResponse> opponentTeamVisible,
        List<GuessLogResponse> logs
) {
    public record MatchSlotResponse(
            Integer slotIndex,
            String pokemonId,
            String pokemonName,
            Boolean discovered
    ) {
    }

    public record GuessLogResponse(
            String side,
            String pokemonName,
            Integer hitCount,
            String exactSlots,
            String generationSlots,
            String typeSlots,
            String colorSlots,
            String heightSlots,
            String weightSlots
    ) {
    }
}
