package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameResults;
import org.springframework.http.HttpStatus;

/**
 * Valida placar e coerência do resultado (perspetiva do utilizador autenticado).
 */
public final class GameFinishValidation {

    private GameFinishValidation() {
    }

    public static void validateScores(int userCorrectGuesses, int opponentCorrectGuesses) {
        if (userCorrectGuesses < 0 || opponentCorrectGuesses < 0) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_CORRECT_GUESSES_INVALID,
                    MessageKeys.GAME_CORRECT_GUESSES_MIN
            );
        }
        if (userCorrectGuesses > GameConstants.MAX_CORRECT_GUESSES
                || opponentCorrectGuesses > GameConstants.MAX_CORRECT_GUESSES) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_CORRECT_GUESSES_INVALID,
                    MessageKeys.GAME_CORRECT_GUESSES_MAX,
                    GameConstants.MAX_CORRECT_GUESSES
            );
        }
    }

    public static void validateResult(int userCorrectGuesses, int opponentCorrectGuesses, GameResults result) {
        validateScores(userCorrectGuesses, opponentCorrectGuesses);
        if (result == null) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_RESULT_REQUIRED,
                    MessageKeys.GAME_RESULT_REQUIRED
            );
        }
        if (result == GameResults.DESISTENCE) {
            return;
        }
        boolean consistent = switch (result) {
            case WIN -> userCorrectGuesses > opponentCorrectGuesses;
            case LOSE -> userCorrectGuesses < opponentCorrectGuesses;
            case DRAW -> userCorrectGuesses == opponentCorrectGuesses;
            case DESISTENCE -> true;
        };
        if (!consistent) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_RESULT_INCONSISTENT,
                    MessageKeys.GAME_RESULT_INCONSISTENT
            );
        }
    }

    public static String normalizeOpponentName(String opponentName) {
        if (opponentName == null) {
            return null;
        }
        String trimmed = opponentName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static void validateLocalOpponentName(String opponentName) {
        String normalized = normalizeOpponentName(opponentName);
        if (normalized == null || normalized.length() < GameConstants.LOCAL_OPPONENT_NAME_MIN_LENGTH) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_OPPONENT_NAME_REQUIRED,
                    MessageKeys.GAME_OPPONENT_NAME_REQUIRED
            );
        }
        if (normalized.length() > GameConstants.OPPONENT_NAME_MAX_LENGTH) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_OPPONENT_NAME_INVALID,
                    MessageKeys.GAME_OPPONENT_NAME_SIZE,
                    GameConstants.OPPONENT_NAME_MAX_LENGTH
            );
        }
    }

    public static String validateAndNormalizeLocalOpponentName(String opponentName) {
        validateLocalOpponentName(opponentName);
        return normalizeOpponentName(opponentName);
    }

    public static GameResults opponentResult(GameResults userResult) {
        return switch (userResult) {
            case WIN -> GameResults.LOSE;
            case LOSE -> GameResults.WIN;
            case DRAW -> GameResults.DRAW;
            case DESISTENCE -> GameResults.WIN;
        };
    }
}
