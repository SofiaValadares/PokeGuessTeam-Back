package com.svc.pokeguessteam.config.seed;

import com.svc.pokeguessteam.model.EvolutionLineModel;
import com.svc.pokeguessteam.model.enums.PokemonRarity;

import java.util.List;

public record EvolutionLineSeedEntry(
        int lineKey,
        PokemonRarity lineRarity,
        List<Integer> memberPokedexNumbers
) {
    public static EvolutionLineSeedEntry line(
            int lineKey,
            PokemonRarity lineRarity,
            List<Integer> memberPokedexNumbers
    ) {
        return new EvolutionLineSeedEntry(lineKey, lineRarity, memberPokedexNumbers);
    }

    public EvolutionLineModel toModel() {
        EvolutionLineModel m = new EvolutionLineModel();
        m.setLineKey(lineKey);
        m.setRarity(lineRarity);
        m.setMemberPokedexNumbers(memberPokedexNumbers == null ? List.of() : List.copyOf(memberPokedexNumbers));
        return m;
    }
}
