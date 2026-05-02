package com.svc.pokeguessteam.dto.pokemon;

import com.svc.pokeguessteam.model.enums.PokemonRarity;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;

import java.util.List;

public record EvolutionLineDto(
        Integer key,
        PokemonRarity rarity,
        List<Integer> members
) {
    public static EvolutionLineDto from(EvolutionLineModel line) {
        if (line == null) {
            return null;
        }
        return new EvolutionLineDto(
                line.getLineKey(),
                line.getRarity(),
                List.copyOf(line.getMemberPokedexNumbers())
        );
    }
}
