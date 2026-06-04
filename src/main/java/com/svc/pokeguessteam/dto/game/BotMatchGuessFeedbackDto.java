package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.GuessOutcome;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;

import java.time.LocalDateTime;
import java.util.List;

public record BotMatchGuessFeedbackDto(
        String id,
        MatchPlayerSide playerSide,
        int guessedPokedexNumber,
        String guessedPokemonName,
        boolean exactMatch,
        List<Integer> matchedPokedexNumbers,
        GuessOutcome outcome,
        String message,
        LocalDateTime createdAt,
        boolean timedOut,
        boolean autoSelected
) {
    public static BotMatchGuessFeedbackDto from(
            ActiveMatchGuessModel guess,
            String pokemonName,
            GuessOutcome outcome,
            String message
    ) {
        return new BotMatchGuessFeedbackDto(
                guess.getId(),
                guess.getPlayerSide(),
                guess.getGuessedPokedexNumber(),
                pokemonName,
                guess.isExactMatch(),
                List.copyOf(guess.getMatchedPokedexNumbers()),
                outcome,
                message,
                guess.getCreatedAt(),
                guess.isTimedOut(),
                guess.isAutoSelected()
        );
    }
}
