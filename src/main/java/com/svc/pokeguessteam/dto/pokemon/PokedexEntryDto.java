package com.svc.pokeguessteam.dto.pokemon;

import com.svc.pokeguessteam.model.pokemon.PokemonModel;

public record PokedexEntryDto(
        PokemonDto pokemon,
        boolean registeredInUserPokedex
) {
    public static PokedexEntryDto from(PokemonModel pokemon, boolean registeredInUserPokedex) {
        return new PokedexEntryDto(PokemonDto.from(pokemon), registeredInUserPokedex);
    }
}
