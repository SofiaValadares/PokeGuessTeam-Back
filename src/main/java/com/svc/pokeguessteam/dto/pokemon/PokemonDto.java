package com.svc.pokeguessteam.dto.pokemon;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonRarity;
import com.svc.pokeguessteam.model.enums.PokemonType;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PokemonDto(
        String id,
        Integer number,
        String name,
        PokemonType primaryType,
        PokemonType secondaryType,
        Integer generation,
        PokedexColor color,
        Double heightM,
        Double weightKg,
        PokemonRarity rarity,
        EvolutionStage evolutionStage,
        Integer evolutionLevel,
        EvolutionLineDto evolutionLine
) {
    public static PokemonDto from(PokemonModel pokemon) {
        return new PokemonDto(
                String.valueOf(pokemon.getPokedexNumber()),
                pokemon.getPokedexNumber(),
                pokemon.getName(),
                pokemon.getPrimaryType(),
                pokemon.getSecondaryType(),
                pokemon.getGeneration(),
                pokemon.getColor(),
                pokemon.getHeightM(),
                pokemon.getWeightKg(),
                pokemon.getRarity(),
                pokemon.getEvolutionStage(),
                pokemon.getEvolutionLevel(),
                EvolutionLineDto.from(pokemon.getEvolutionLine())
        );
    }
}
