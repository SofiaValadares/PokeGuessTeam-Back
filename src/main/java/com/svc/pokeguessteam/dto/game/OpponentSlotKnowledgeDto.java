package com.svc.pokeguessteam.dto.game;

/**
 * Um slot (1–6) do time adversário: se já foi adivinhado e as pistas descobertas acumuladas.
 */
public record OpponentSlotKnowledgeDto(
        int slot,
        boolean adivinhado,
        DiscoveredPokemonHintsDto informacoes
) {
}
