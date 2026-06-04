package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameMatchRewardsTest {

    @Test
    void friendWin_grantsPokeBallAnd300Xp() {
        var p = GameMatchRewards.payout(GameModes.FRIEND, GameResults.WIN);
        assertEquals(300, p.trainingTeamXp());
        assertEquals(1, p.pokeBalls());
        assertEquals(0, p.pokeballFragments());
    }

    @Test
    void friendLoseOrDraw_grantsFragmentsAnd150Xp() {
        for (GameResults r : new GameResults[]{GameResults.LOSE, GameResults.DRAW, GameResults.DESISTENCE}) {
            var p = GameMatchRewards.payout(GameModes.FRIEND, r);
            assertEquals(150, p.trainingTeamXp(), r.name());
            assertEquals(0, p.pokeBalls(), r.name());
            assertEquals(5, p.pokeballFragments(), r.name());
        }
    }

    @Test
    void botWin_grantsFragmentsAnd150Xp() {
        var p = GameMatchRewards.payout(GameModes.BOT, GameResults.WIN);
        assertEquals(150, p.trainingTeamXp());
        assertEquals(0, p.pokeBalls());
        assertEquals(5, p.pokeballFragments());
    }

    @Test
    void botLoseOrDraw_grants75XpOnly() {
        for (GameResults r : new GameResults[]{GameResults.LOSE, GameResults.DRAW, GameResults.DESISTENCE}) {
            var p = GameMatchRewards.payout(GameModes.BOT, r);
            assertEquals(75, p.trainingTeamXp(), r.name());
            assertEquals(0, p.pokeBalls(), r.name());
            assertEquals(0, p.pokeballFragments(), r.name());
        }
    }
}
