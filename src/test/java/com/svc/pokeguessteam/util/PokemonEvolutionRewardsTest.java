package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.PokeballType;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void pendingMilestones_excludesClaimedAndLocked() {
        assertEquals(List.of(50), PokemonEvolutionRewards.pendingMilestones(50, List.of(25)));
        assertTrue(PokemonEvolutionRewards.pendingMilestones(24, List.of()).isEmpty());
        assertTrue(PokemonEvolutionRewards.pendingMilestones(100, List.of(25, 50, 75, 100)).isEmpty());
    }

    @Test
    void rewardsForMilestone_level100_grantsThreeBallTypes() {
        var grants = PokemonEvolutionRewards.rewardsForMilestone(100);
        assertEquals(1, grants.get(PokeballType.GREAT_BALL));
        assertEquals(1, grants.get(PokeballType.ULTRA_BALL));
        assertEquals(1, grants.get(PokeballType.MASTER_BALL));
    }
}
