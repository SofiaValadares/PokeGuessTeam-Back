package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.util.GameConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GameLocalFinishRequest(
        @NotBlank(message = "{error.game.opponent-name.required}")
        @Size(
                min = GameConstants.LOCAL_OPPONENT_NAME_MIN_LENGTH,
                max = GameConstants.OPPONENT_NAME_MAX_LENGTH,
                message = "{error.game.opponent-name.size}"
        )
        String opponentName,
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
