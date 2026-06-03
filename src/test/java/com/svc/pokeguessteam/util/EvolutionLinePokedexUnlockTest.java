package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvolutionLinePokedexUnlockTest {

    @Test
    void level1UnlocksOnlyBaseForm() {
        List<PokemonModel> chain = bulbasaurLine();
        UserPokemonInventoryModel row = inventoryRow(0);

        List<PokemonModel> unlocked = EvolutionLinePokedexUnlock.speciesUnlockedAtInventoryLevel(chain, row);

        assertEquals(1, unlocked.size());
        assertEquals(1, unlocked.get(0).getPokedexNumber());
    }

    @Test
    void level16UnlocksSecondStage() {
        List<PokemonModel> chain = bulbasaurLine();
        UserPokemonInventoryModel row = inventoryRow(PokemonInventoryXp.cumulativeXpForLevel(16));

        List<PokemonModel> unlocked = EvolutionLinePokedexUnlock.speciesUnlockedAtInventoryLevel(chain, row);

        assertEquals(2, unlocked.size());
        assertEquals(2, unlocked.get(1).getPokedexNumber());
    }

    private static List<PokemonModel> bulbasaurLine() {
        EvolutionLineModel line = new EvolutionLineModel();
        line.setLineKey(1);
        return List.of(
                species(1, EvolutionStage.BASE, 16),
                species(2, EvolutionStage.FIRST_STAGE, 32),
                species(3, EvolutionStage.SECOND_STAGE, null)
        );
    }

    private static PokemonModel species(int dex, EvolutionStage stage, Integer evolutionLevel) {
        PokemonModel p = new PokemonModel();
        p.setPokedexNumber(dex);
        p.setEvolutionStage(stage);
        p.setEvolutionLevel(evolutionLevel);
        return p;
    }

    private static UserPokemonInventoryModel inventoryRow(int totalXp) {
        UserPokemonInventoryModel row = new UserPokemonInventoryModel();
        row.setTotalXp(totalXp);
        row.setEvolutionLine(new EvolutionLineModel());
        PokemonInventoryXp.syncLevelFromTotalXp(row);
        return row;
    }
}
