package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;

/**
 * Progressão de XP do inventário por linha evolutiva:
 * <ul>
 *     <li>Para subir do nível {@code L} para {@code L+1} é preciso acumular {@code 50 * L} de XP nesse degrau.</li>
 *     <li>1→2: 50 XP; 2→3: 100 XP; 3→4: 150 XP; …</li>
 * </ul>
 * O campo {@link UserPokemonInventoryModel#getTotalXp() totalXp} é o XP acumulado; o {@link UserPokemonInventoryModel#getLevel() nível}
 * deve refletir esse total (use {@link #syncLevelFromTotalXp} ou {@link #addXpAndSyncLevel}).
 * O nível máximo é {@value #MAX_LEVEL}.
 */
public final class PokemonInventoryXp {

    /** XP necessário só no primeiro degrau (nível 1 → 2). */
    public static final int FIRST_LEVEL_UP_XP = 50;

    /** Nível máximo por linha no inventário. */
    public static final int MAX_LEVEL = 100;

    private PokemonInventoryXp() {
    }

    /**
     * XP total acumulado necessário para <strong>atingir</strong> o nível dado (nível 1 corresponde a 0 XP).
     */
    public static int cumulativeXpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        return 25 * level * (level - 1);
    }

    /**
     * XP a ganhar no degrau atual (do nível {@code currentLevel} para o seguinte): {@code 50 * currentLevel}.
     * No nível máximo devolve {@code 0}.
     */
    public static int xpForStepFromLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return 0;
        }
        return FIRST_LEVEL_UP_XP * Math.max(1, currentLevel);
    }

    /**
     * Nível derivado do XP total acumulado, até {@value #MAX_LEVEL}.
     */
    public static int levelFromTotalXp(int totalXp) {
        int xp = Math.max(0, totalXp);
        int level = 1;
        while (level < MAX_LEVEL && cumulativeXpForLevel(level + 1) <= xp) {
            level++;
        }
        return level;
    }

    /**
     * Quanto XP falta para o próximo nível, dado o total já acumulado.
     * No nível máximo devolve {@code 0}.
     */
    public static int xpRemainingToNextLevel(int totalXp) {
        int xp = Math.max(0, totalXp);
        int level = levelFromTotalXp(xp);
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return Math.max(0, cumulativeXpForLevel(level + 1) - xp);
    }

    public static void syncLevelFromTotalXp(UserPokemonInventoryModel row) {
        int xp = row.getTotalXp() != null ? row.getTotalXp() : 0;
        row.setLevel(levelFromTotalXp(xp));
    }

    /**
     * Soma XP (não negativo) e atualiza o nível em conformidade.
     */
    public static void addXpAndSyncLevel(UserPokemonInventoryModel row, int xpGained) {
        int current = row.getTotalXp() != null ? row.getTotalXp() : 0;
        int delta = Math.max(0, xpGained);
        row.setTotalXp(current + delta);
        syncLevelFromTotalXp(row);
    }
}
