package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.util.BotAiOpponent;

public record OpponentKnowledgeSlotDto(
        Integer pokedexNumber,
        boolean revealed,
        String primaryType,
        String secondaryType,
        String color,
        String generation,
        String heightM,
        String weightKg
) {
    public static OpponentKnowledgeSlotDto from(BotAiOpponent.OpponentKnowledgeSlot slot) {
        return new OpponentKnowledgeSlotDto(
                slot.pokedexNumber(),
                slot.revealed(),
                slot.primaryType(),
                slot.secondaryType(),
                slot.color(),
                slot.generation(),
                slot.heightM(),
                slot.weightKg()
        );
    }
}
