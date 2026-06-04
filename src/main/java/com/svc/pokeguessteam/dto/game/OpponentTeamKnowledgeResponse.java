package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.util.GameConstants;

import java.util.List;

/**
 * Conhecimento do jogador da vez sobre o time adversário (6 slots).
 */
public record OpponentTeamKnowledgeResponse(
        MatchPlayerSide jogadorDaVez,
        List<OpponentSlotKnowledgeDto> slots
) {
    public OpponentTeamKnowledgeResponse {
        if (slots == null) {
            slots = List.of();
        }
        if (slots.size() != GameConstants.TEAM_SIZE) {
            throw new IllegalArgumentException("Esperados " + GameConstants.TEAM_SIZE + " slots");
        }
    }
}
