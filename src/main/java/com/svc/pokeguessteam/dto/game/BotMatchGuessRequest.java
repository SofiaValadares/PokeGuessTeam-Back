package com.svc.pokeguessteam.dto.game;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BotMatchGuessRequest(
        @NotNull @Min(1) Integer pokedexNumber
) {
}
