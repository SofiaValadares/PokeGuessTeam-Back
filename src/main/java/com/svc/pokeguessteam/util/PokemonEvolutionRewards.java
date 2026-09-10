package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.PokeballType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recompensas ao atingir marcos de nível numa linha do inventário (PC).
 */
public final class PokemonEvolutionRewards {

    public static final List<Integer> ALL_MILESTONES = List.of(25, 50, 75, 100);

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

    public static List<Integer> parseClaimed(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Integer> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                out.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
                // ignora tokens inválidos
            }
        }
        return List.copyOf(out);
    }

    public static String formatClaimed(Collection<Integer> claimed) {
        if (claimed == null || claimed.isEmpty()) {
            return "";
        }
        return claimed.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /** Marcos já atingidos pelo nível actual. */
    public static List<Integer> milestonesReached(int level) {
        return ALL_MILESTONES.stream().filter(m -> level >= m).toList();
    }

    /** Marcos atingidos mas ainda não resgatados manualmente. */
    public static List<Integer> pendingMilestones(int level, Collection<Integer> claimed) {
        Set<Integer> claimedSet = claimed instanceof Set<Integer> s ? s : new LinkedHashSet<>(claimed);
        List<Integer> pending = new ArrayList<>();
        for (int milestone : ALL_MILESTONES) {
            if (level >= milestone && !claimedSet.contains(milestone)) {
                pending.add(milestone);
            }
        }
        return List.copyOf(pending);
    }

    /** Bolas concedidas ao resgatar um marco específico. */
    public static Map<PokeballType, Integer> rewardsForMilestone(int milestone) {
        return switch (milestone) {
            case 25 -> Map.of(PokeballType.GREAT_BALL, 1);
            case 50 -> Map.of(PokeballType.ULTRA_BALL, 1);
            case 75 -> Map.of(PokeballType.MASTER_BALL, 1);
            case 100 -> Map.of(
                    PokeballType.GREAT_BALL, 1,
                    PokeballType.ULTRA_BALL, 1,
                    PokeballType.MASTER_BALL, 1
            );
            default -> Map.of();
        };
    }

    public static Map<PokeballType, Integer> mergeRewards(
            Map<PokeballType, Integer> base,
            Map<PokeballType, Integer> extra
    ) {
        if (extra.isEmpty()) {
            return base;
        }
        EnumMap<PokeballType, Integer> merged = new EnumMap<>(PokeballType.class);
        merged.putAll(base);
        for (var entry : extra.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return Map.copyOf(merged);
    }

    private static void add(EnumMap<PokeballType, Integer> grants, PokeballType type, int delta) {
        grants.merge(type, delta, Integer::sum);
    }
}
