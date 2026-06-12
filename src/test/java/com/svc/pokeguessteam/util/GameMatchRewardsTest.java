package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameMatchRewardsTest {

    @Test
    void friendWin_grantsPokeBallAnd1000Xp() {
        var p = GameMatchRewards.payout(GameModes.FRIEND, GameResults.WIN);
        assertEquals(1000, p.trainingTeamXp());
        assertEquals(1, p.pokeBalls());
        assertEquals(0, p.pokeballFragments());
    }

    @Test
    void friendLoseOrDraw_grants500Xp() {
        for (GameResults r : new GameResults[]{GameResults.LOSE, GameResults.DRAW, GameResults.DESISTENCE}) {
            var p = GameMatchRewards.payout(GameModes.FRIEND, r);
            assertEquals(500, p.trainingTeamXp(), r.name());
            assertEquals(0, p.pokeBalls(), r.name());
            assertEquals(5, p.pokeballFragments(), r.name());
        }
    }

    @Test
    void botWin_grants700Xp() {
        var p = GameMatchRewards.payout(GameModes.BOT, GameResults.WIN);
        assertEquals(700, p.trainingTeamXp());
        assertEquals(0, p.pokeBalls());
        assertEquals(5, p.pokeballFragments());
    }

    @Test
    void botLoseOrDraw_grants350Xp() {
        for (GameResults r : new GameResults[]{GameResults.LOSE, GameResults.DRAW, GameResults.DESISTENCE}) {
            var p = GameMatchRewards.payout(GameModes.BOT, r);
            assertEquals(350, p.trainingTeamXp(), r.name());
            assertEquals(0, p.pokeBalls(), r.name());
            assertEquals(0, p.pokeballFragments(), r.name());
        }
    }

    @Test
    void localMatch_grantsNothing() {
        for (GameResults r : GameResults.values()) {
            var p = GameMatchRewards.payout(GameModes.LOCAL, r);
            assertEquals(0, p.trainingTeamXp(), r.name());
            assertEquals(0, p.pokeBalls(), r.name());
            assertEquals(0, p.pokeballFragments(), r.name());
        }
    }
}
