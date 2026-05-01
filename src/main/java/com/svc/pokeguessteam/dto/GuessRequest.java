package com.svc.pokeguessteam.dto;

import jakarta.validation.constraints.NotBlank;

public record GuessRequest(
        @NotBlank
        String guessedPokemonId
) {
}
