package com.svc.pokeguessteam.dto;

import java.util.List;

public record GuessResultResponse(
        String matchId,
        String guessedPokemonId,
        String guessedPokemonName,
        List<Integer> exactHitSlots,
        List<Integer> generationMatchSlots,
        List<Integer> typeMatchSlots,
        List<Integer> colorMatchSlots,
        List<Integer> heightMatchSlots,
        List<Integer> weightMatchSlots,
        String nextTurnSide,
        Integer playerScore,
        Integer opponentScore,
        String status,
        String winnerUserId
) {
}
