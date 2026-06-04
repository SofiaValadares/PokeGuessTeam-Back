package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.util.GameConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Equipa de um dos dois jogadores no modo local (pass-and-play).
 * {@code playerSide}: {@link MatchPlayerSide#HOST} (conta) ou {@link MatchPlayerSide#OPPONENT} (2.º jogador).
 * Aceita aliases legados {@code USER} / {@code BOT}.
 */
public record LocalMatchTeamRequest(
        @NotNull
        MatchPlayerSide playerSide,
        @NotNull
        @Size(min = GameConstants.TEAM_SIZE, max = GameConstants.TEAM_SIZE)
        List<Integer> team
) {
}
