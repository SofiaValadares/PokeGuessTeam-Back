package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import org.springframework.http.HttpStatus;

/**
 * Resultado vs bot vem do MatchEngine no servidor; o cliente só pode desistir
 * enquanto a partida ainda está activa.
 */
public final class BotMatchFinishResolver {

    private BotMatchFinishResolver() {
    }

    public static GameResults resolve(MatchStatus status, MatchPlayerSide winner, GameResults claimed) {
        if (status != MatchStatus.FINISHED) {
            if (claimed == GameResults.DESISTENCE) {
                return GameResults.DESISTENCE;
            }
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_PHASE,
                    MessageKeys.GAME_MATCH_INVALID_PHASE
            );
        }
        if (winner == MatchPlayerSide.HOST) {
            return GameResults.WIN;
        }
        if (winner == MatchPlayerSide.OPPONENT) {
            return GameResults.LOSE;
        }
        return GameResults.DRAW;
    }
}
