package com.svc.pokeguessteam.dto.game;

public record BotMatchGuessCheckResponse(
        boolean exactMatch,
        int pokedexNumber
) {
}
