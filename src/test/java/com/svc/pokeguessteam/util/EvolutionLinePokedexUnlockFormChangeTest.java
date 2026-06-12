package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EvolutionLinePokedexUnlockFormChangeTest {

    @Test
    void level16_froakieLine_changesToFrogadier() {
        List<PokemonModel> chain = froakieLine();
        EvolutionLinePokedexUnlock.FormChange change = EvolutionLinePokedexUnlock.formChangeBetweenLevels(chain, 15, 16);

        assertEquals(656, change.fromDex());
        assertEquals(657, change.toDex());
    }

    @Test
    void sameFormAcrossLevels_returnsNull() {
        List<PokemonModel> chain = froakieLine();
        assertNull(EvolutionLinePokedexUnlock.formChangeBetweenLevels(chain, 10, 15));
    }

    @Test
    void displayFormDexAtLevel16_isFrogadier() {
        List<PokemonModel> chain = froakieLine();
        assertEquals(657, EvolutionLinePokedexUnlock.displayFormDexAtLevel(chain, 16));
    }

    private static List<PokemonModel> froakieLine() {
        return List.of(
                species(656, EvolutionStage.BASE, 16),
                species(657, EvolutionStage.FIRST_STAGE, 36),
                species(658, EvolutionStage.SECOND_STAGE, null)
        );
    }

    private static PokemonModel species(int dex, EvolutionStage stage, Integer evolutionLevel) {
        PokemonModel p = new PokemonModel();
        p.setPokedexNumber(dex);
        p.setEvolutionStage(stage);
        p.setEvolutionLevel(evolutionLevel);
        EvolutionLineModel line = new EvolutionLineModel();
        line.setLineKey(354);
        p.setEvolutionLine(line);
        return p;
    }
}
