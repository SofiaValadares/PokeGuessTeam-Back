package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * XP, desbloqueio da linha evolutiva e recompensas de marco.
 */
class PokemonProgressionRulesTest {

    @Nested
    class PokedexUnlock {

        @ParameterizedTest
        @CsvSource({
                "1, 1",
                "15, 1",
                "16, 2",
                "31, 2",
                "32, 3"
        })
        void unlocksTheNextFormWhenThePreviousEvolutionLevelIsReached(int level, int unlockedCount) {
            UserPokemonInventoryModel row = inventoryRow(PokemonInventoryXp.cumulativeXpForLevel(level));
            List<PokemonModel> unlocked = EvolutionLinePokedexUnlock.speciesUnlockedAtInventoryLevel(
                    bulbasaurLine(),
                    row
            );
            assertEquals(unlockedCount, unlocked.size());
            assertEquals(1, unlocked.get(0).getPokedexNumber());
            if (unlockedCount >= 2) {
                assertEquals(2, unlocked.get(1).getPokedexNumber());
            }
            if (unlockedCount >= 3) {
                assertEquals(3, unlocked.get(2).getPokedexNumber());
            }
        }
    }

    @Nested
    class MilestoneRewards {

        @Test
        void crossingMilestonesGrantsTheExpectedBallsAndIgnoresAlreadyClaimedOnes() {
            var at25 = PokemonEvolutionRewards.ballsForLevelCrossing(24, 25);
            assertEquals(1, at25.get(PokeballType.GREAT_BALL));
            assertEquals(1, at25.size());

            var at100 = PokemonEvolutionRewards.ballsForLevelCrossing(99, 100);
            assertEquals(1, at100.get(PokeballType.GREAT_BALL));
            assertEquals(1, at100.get(PokeballType.ULTRA_BALL));
            assertEquals(1, at100.get(PokeballType.MASTER_BALL));
            assertEquals(3, at100.size());
            var milestone100 = PokemonEvolutionRewards.rewardsForMilestone(100);
            assertEquals(1, milestone100.get(PokeballType.MASTER_BALL));

            assertEquals(List.of(50), PokemonEvolutionRewards.pendingMilestones(50, List.of(25)));
            assertTrue(PokemonEvolutionRewards.pendingMilestones(24, List.of()).isEmpty());
            assertTrue(PokemonEvolutionRewards.pendingMilestones(100, List.of(25, 50, 75, 100)).isEmpty());
        }
    }

    private static List<PokemonModel> bulbasaurLine() {
        return List.of(
                species(1, EvolutionStage.BASE, 16),
                species(2, EvolutionStage.FIRST_STAGE, 32),
                species(3, EvolutionStage.SECOND_STAGE, null)
        );
    }

    private static PokemonModel species(int dex, EvolutionStage stage, Integer evolutionLevel) {
        PokemonModel pokemon = new PokemonModel();
        pokemon.setPokedexNumber(dex);
        pokemon.setEvolutionStage(stage);
        pokemon.setEvolutionLevel(evolutionLevel);
        return pokemon;
    }

    private static UserPokemonInventoryModel inventoryRow(int totalXp) {
        UserPokemonInventoryModel row = new UserPokemonInventoryModel();
        row.setTotalXp(totalXp);
        row.setEvolutionLine(new EvolutionLineModel());
        PokemonInventoryXp.syncLevelFromTotalXp(row);
        return row;
    }
}
