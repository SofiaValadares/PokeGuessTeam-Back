package com.svc.pokeguessteam.config.seed;

import com.svc.pokeguessteam.model.EvolutionLineModel;
import com.svc.pokeguessteam.model.PokemonModel;
import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonType;

public record PokemonSeedEntry(
        int pokedexNumber,
        String name,
        PokemonType primaryType,
        PokemonType secondaryType,
        int generation,
        PokedexColor color,
        double heightM,
        double weightKg,
        EvolutionStage evolutionStage,
        Integer evolutionLevel,
        int evolutionLineKey
) {
    public static PokemonSeedEntry pokemon(
            int pokedexNumber,
            String name,
            PokemonType primaryType,
            PokemonType secondaryType,
            int generation,
            PokedexColor color,
            double heightM,
            double weightKg,
            EvolutionStage evolutionStage,
            Integer evolutionLevel,
            int evolutionLineKey
    ) {
        return new PokemonSeedEntry(
                pokedexNumber,
                name,
                primaryType,
                secondaryType,
                generation,
                color,
                heightM,
                weightKg,
                evolutionStage,
                evolutionLevel,
                evolutionLineKey
        );
    }

    public PokemonModel toModel(EvolutionLineModel evolutionLine) {
        PokemonModel m = new PokemonModel();
        m.setPokedexNumber(pokedexNumber);
        m.setName(name);
        m.setPrimaryType(primaryType);
        m.setSecondaryType(secondaryType);
        m.setGeneration(generation);
        m.setColor(color);
        m.setHeightM(heightM);
        m.setWeightKg(weightKg);
        m.setEvolutionLine(evolutionLine);
        m.setEvolutionStage(evolutionStage);
        m.setEvolutionLevel(evolutionLevel);

        return m;
    }
}
