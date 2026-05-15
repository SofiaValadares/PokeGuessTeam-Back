package com.svc.pokeguessteam.dto.pokemon;

import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import com.svc.pokeguessteam.util.PokemonInventoryXp;

import java.util.List;

/**
 * Uma linha do PC do jogador: progresso por linha evolutiva (espécies obtidas / XP).
 */
public record PcLineDto(
        Integer evolutionLineKey,
        List<Integer> members,
        String rarity,
        int level,
        int totalXp,
        int xpToNextLevel,
        int xpForCurrentStep,
        int timesObtained
) {
    public static PcLineDto from(UserPokemonInventoryModel row) {
        EvolutionLineModel line = row.getEvolutionLine();
        int xp = row.getTotalXp() != null ? row.getTotalXp() : 0;
        int level = PokemonInventoryXp.levelFromTotalXp(xp);
        int times = row.getTimesObtained() != null ? row.getTimesObtained() : 0;
        return new PcLineDto(
                line.getLineKey(),
                List.copyOf(line.getMemberPokedexNumbers()),
                line.getRarity().name(),
                level,
                xp,
                PokemonInventoryXp.xpRemainingToNextLevel(xp),
                PokemonInventoryXp.xpForStepFromLevel(level),
                times
        );
    }
}
