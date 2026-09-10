package com.svc.pokeguessteam.dto.pokemon;

import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.enums.PokemonRarity;

public record PokeballDrawResponse(
        PokeballType pokeballType,
        PokemonRarity rolledRarity,
        PokemonDto pokemon,
        boolean newInventoryLine,
        int timesObtainedOnLine
) {
}
