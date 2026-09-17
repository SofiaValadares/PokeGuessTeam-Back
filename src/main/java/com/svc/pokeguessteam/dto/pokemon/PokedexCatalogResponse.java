package com.svc.pokeguessteam.dto.pokemon;

import java.util.List;

public record PokedexCatalogResponse(
        String pokedexVersion,
        List<PokemonDto> species,
        List<EvolutionLineDto> evolutionLines
) {
}
