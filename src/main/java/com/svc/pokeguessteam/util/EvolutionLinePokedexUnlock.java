package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Quais espécies de uma linha evolutiva devem aparecer na Pokédex pessoal,
 * consoante o nível de XP da linha no inventário.
 */
public final class EvolutionLinePokedexUnlock {

    private EvolutionLinePokedexUnlock() {
    }

    public static List<PokemonModel> speciesUnlockedAtInventoryLevel(
            List<PokemonModel> lineSpecies,
            UserPokemonInventoryModel inventoryRow
    ) {
        if (lineSpecies == null || lineSpecies.isEmpty()) {
            return List.of();
        }
        List<PokemonModel> chain = new ArrayList<>(lineSpecies);
        chain.sort(evolutionOrderComparator());

        int playerLevel = PokemonInventoryXp.levelFromTotalXp(
                inventoryRow.getTotalXp() != null ? inventoryRow.getTotalXp() : 0
        );

        List<PokemonModel> unlocked = new ArrayList<>();
        unlocked.add(chain.get(0));

        for (int i = 1; i < chain.size(); i++) {
            PokemonModel previous = chain.get(i - 1);
            Integer requiredLevel = previous.getEvolutionLevel();
            if (requiredLevel == null || playerLevel >= requiredLevel) {
                unlocked.add(chain.get(i));
            }
        }
        return unlocked;
    }

    private static Comparator<PokemonModel> evolutionOrderComparator() {
        return Comparator
                .comparingInt(EvolutionLinePokedexUnlock::stageRank)
                .thenComparing(PokemonModel::getPokedexNumber);
    }

    private static int stageRank(PokemonModel pokemon) {
        EvolutionStage stage = pokemon.getEvolutionStage();
        if (stage == null) {
            return 99;
        }
        return switch (stage) {
            case BASE -> 0;
            case FIRST_STAGE -> 1;
            case SECOND_STAGE -> 2;
        };
    }
}
