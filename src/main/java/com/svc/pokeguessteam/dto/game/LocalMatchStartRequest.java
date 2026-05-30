package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.util.GameConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalMatchStartRequest(
        @NotBlank(message = "{error.game.opponent-name.required}")
        @Size(
                min = GameConstants.LOCAL_OPPONENT_NAME_MIN_LENGTH,
                max = GameConstants.OPPONENT_NAME_MAX_LENGTH,
                message = "{error.game.opponent-name.size}"
        )
        String opponentName
) {
}
