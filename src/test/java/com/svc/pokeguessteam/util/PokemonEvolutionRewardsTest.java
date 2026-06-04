package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.PokeballType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokemonEvolutionRewardsTest {

    @Test
    void crossingLevel25_grantsGreatBall() {
        var grants = PokemonEvolutionRewards.ballsForLevelCrossing(24, 25);
        assertEquals(1, grants.get(PokeballType.GREAT_BALL));
        assertEquals(1, grants.size());
    }

    @Test
    void crossingLevel100_grantsAllThreeBallTypes() {
        var grants = PokemonEvolutionRewards.ballsForLevelCrossing(99, 100);
        assertEquals(1, grants.get(PokeballType.GREAT_BALL));
        assertEquals(1, grants.get(PokeballType.ULTRA_BALL));
        assertEquals(1, grants.get(PokeballType.MASTER_BALL));
        assertEquals(3, grants.size());
    }

    @Test
    void noLevelChange_grantsNothing() {
        assertTrue(PokemonEvolutionRewards.ballsForLevelCrossing(50, 50).isEmpty());
    }
}
