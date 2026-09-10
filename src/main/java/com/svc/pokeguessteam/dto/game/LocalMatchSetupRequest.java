package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.util.GameConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LocalMatchSetupRequest(
        @NotBlank(message = "{error.game.opponent-name.required}")
        @Size(
                min = GameConstants.LOCAL_OPPONENT_NAME_MIN_LENGTH,
                max = GameConstants.OPPONENT_NAME_MAX_LENGTH,
                message = "{error.game.opponent-name.size}"
        )
        String opponentName,
        @NotNull(message = "{error.game.team.required}")
        @Size(min = GameConstants.TEAM_SIZE, max = GameConstants.TEAM_SIZE, message = "{error.game.team.size}")
        List<Integer> hostTeam,
        @NotNull(message = "{error.game.team.required}")
        @Size(min = GameConstants.TEAM_SIZE, max = GameConstants.TEAM_SIZE, message = "{error.game.team.size}")
        List<Integer> opponentTeam
) {
}
