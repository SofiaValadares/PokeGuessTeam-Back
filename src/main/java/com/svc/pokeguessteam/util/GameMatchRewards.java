package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;

/**
 * Recompensas ao fim da partida (XP no time de treino, Pokébolas e fragmentos).
 * Cada slot ocupado do time de treino recebe o XP integral ({@link ProfileService#grantTrainingTeamMatchXp}).
 */
public final class GameMatchRewards {

    /** Vitória online. */
    public static final int TRAINING_XP_FRIEND_WIN = 1000;

    /** Derrota/empate/desistência online. */
    public static final int TRAINING_XP_FRIEND_LOSE = TRAINING_XP_FRIEND_WIN / 2;

    /** Vitória vs bot. */
    public static final int TRAINING_XP_BOT_WIN = 700;

    /** Derrota/empate/desistência vs bot. */
    public static final int TRAINING_XP_BOT_LOSE = 350;

    private GameMatchRewards() {
    }

    public record MatchRewardPayout(int trainingTeamXp, int pokeBalls, int pokeballFragments) {
    }

    public static MatchRewardPayout payout(GameModes mode, GameResults result) {
        return switch (mode) {
            case FRIEND -> payoutFriend(result);
            case BOT -> payoutBot(result);
            case LOCAL -> payoutLocal(result);
        };
    }

    private static MatchRewardPayout payoutFriend(GameResults result) {
        return switch (result) {
            case WIN -> new MatchRewardPayout(TRAINING_XP_FRIEND_WIN, 1, 0);
            case DRAW, LOSE, DESISTENCE -> new MatchRewardPayout(TRAINING_XP_FRIEND_LOSE, 0, 5);
        };
    }

    private static MatchRewardPayout payoutBot(GameResults result) {
        return switch (result) {
            case WIN -> new MatchRewardPayout(TRAINING_XP_BOT_WIN, 0, 5);
            case DRAW, LOSE, DESISTENCE -> new MatchRewardPayout(TRAINING_XP_BOT_LOSE, 0, 0);
        };
    }

    private static MatchRewardPayout payoutLocal(GameResults result) {
        return new MatchRewardPayout(0, 0, 0);
    }
}
