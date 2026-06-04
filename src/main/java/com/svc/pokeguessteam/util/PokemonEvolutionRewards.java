package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.PokeballType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Recompensas ao atingir marcos de nível numa linha do inventário (PC).
 */
public final class PokemonEvolutionRewards {

    private PokemonEvolutionRewards() {
    }

    /**
     * Bolas concedidas ao cruzar cada marco (nível anterior &lt; marco ≤ nível novo).
     */
    public static Map<PokeballType, Integer> ballsForLevelCrossing(int oldLevel, int newLevel) {
        EnumMap<PokeballType, Integer> grants = new EnumMap<>(PokeballType.class);
        if (oldLevel < 25 && newLevel >= 25) {
            add(grants, PokeballType.GREAT_BALL, 1);
        }
        if (oldLevel < 50 && newLevel >= 50) {
            add(grants, PokeballType.ULTRA_BALL, 1);
        }
        if (oldLevel < 75 && newLevel >= 75) {
            add(grants, PokeballType.MASTER_BALL, 1);
        }
        if (oldLevel < 100 && newLevel >= 100) {
            add(grants, PokeballType.GREAT_BALL, 1);
            add(grants, PokeballType.ULTRA_BALL, 1);
            add(grants, PokeballType.MASTER_BALL, 1);
        }
        return Map.copyOf(grants);
    }

    public static Map<String, Object> meta() {
        return Map.of(
                "milestones", Map.of(
                        "25", Map.of(PokeballType.GREAT_BALL.name(), 1),
                        "50", Map.of(PokeballType.ULTRA_BALL.name(), 1),
                        "75", Map.of(PokeballType.MASTER_BALL.name(), 1),
                        "100", Map.of(
                                PokeballType.GREAT_BALL.name(), 1,
                                PokeballType.ULTRA_BALL.name(), 1,
                                PokeballType.MASTER_BALL.name(), 1
                        )
                )
        );
    }

    private static void add(EnumMap<PokeballType, Integer> grants, PokeballType type, int delta) {
        grants.merge(type, delta, Integer::sum);
    }
}
