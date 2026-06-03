package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.GameResults;

/**
 * Recompensas de partida (GDD: XP no time de treino e Pokébolas conforme desempenho).
 */
public final class GameMatchRewards {

    private GameMatchRewards() {
    }

    public static int xpForResult(GameResults result) {
        return switch (result) {
            case WIN -> 40;
            case DRAW -> 20;
            case LOSE -> 12;
            case DESISTENCE -> 5;
        };
    }

    public static int pokeBallsForResult(GameResults result) {
        return switch (result) {
            case WIN -> 2;
            case DRAW -> 1;
            case LOSE, DESISTENCE -> 0;
        };
    }
}
