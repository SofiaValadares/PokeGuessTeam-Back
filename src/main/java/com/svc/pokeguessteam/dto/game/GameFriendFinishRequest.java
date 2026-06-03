package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.util.GameConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GameFriendFinishRequest(
        @NotBlank(message = "{error.game.opponent-user-id.required}")
        String opponentUserId,
        @Min(value = 0, message = "{error.game.correct-guesses.min}")
        @Max(value = GameConstants.MAX_CORRECT_GUESSES, message = "{error.game.correct-guesses.max}")
        int userCorrectGuesses,
        @Min(value = 0, message = "{error.game.correct-guesses.min}")
        @Max(value = GameConstants.MAX_CORRECT_GUESSES, message = "{error.game.correct-guesses.max}")
        int opponentCorrectGuesses,
        @NotNull(message = "{error.game.result.required}")
        GameResults result
) implements GameFinishRequest {
}
