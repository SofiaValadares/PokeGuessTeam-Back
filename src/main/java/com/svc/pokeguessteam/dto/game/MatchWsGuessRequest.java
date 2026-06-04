package com.svc.pokeguessteam.dto.game;

import jakarta.validation.constraints.Positive;

public record MatchWsGuessRequest(
        @Positive int pokedexNumber
) {
}
