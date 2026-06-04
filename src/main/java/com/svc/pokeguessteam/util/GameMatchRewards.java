package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;

/**
 * Recompensas ao fim da partida (XP no time de treino, Pokébolas e fragmentos).
 */
public final class GameMatchRewards {

    private GameMatchRewards() {
    }

    public record MatchRewardPayout(int trainingTeamXp, int pokeBalls, int pokeballFragments) {
    }

    public static MatchRewardPayout payout(GameModes mode, GameResults result) {
        return switch (mode) {
            case FRIEND -> payoutFriend(result);
            case BOT, LOCAL -> payoutBotOrLocal(result);
        };
    }

    private static MatchRewardPayout payoutFriend(GameResults result) {
        return switch (result) {
            case WIN -> new MatchRewardPayout(300, 1, 0);
            case DRAW, LOSE, DESISTENCE -> new MatchRewardPayout(150, 0, 5);
        };
    }

    private static MatchRewardPayout payoutBotOrLocal(GameResults result) {
        return switch (result) {
            case WIN -> new MatchRewardPayout(150, 0, 5);
            case DRAW, LOSE, DESISTENCE -> new MatchRewardPayout(75, 0, 0);
        };
    }
}
