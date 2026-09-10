package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.GameResults;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameFinishValidationTest {

    @Test
    void opponentResultInvertsWinAndDesistence() {
        assertEquals(GameResults.LOSE, GameFinishValidation.opponentResult(GameResults.WIN));
        assertEquals(GameResults.WIN, GameFinishValidation.opponentResult(GameResults.LOSE));
        assertEquals(GameResults.DRAW, GameFinishValidation.opponentResult(GameResults.DRAW));
        assertEquals(GameResults.WIN, GameFinishValidation.opponentResult(GameResults.DESISTENCE));
    }

    @Test
    void validateResultAcceptsConsistentWin() {
        GameFinishValidation.validateResult(4, 2, GameResults.WIN);
    }

    @Test
    void validateResultRejectsInconsistentWin() {
        assertThrows(Exception.class, () -> GameFinishValidation.validateResult(2, 4, GameResults.WIN));
    }

    @Test
    void validateResultAllowsDesistenceWithAnyScore() {
        GameFinishValidation.validateResult(0, 6, GameResults.DESISTENCE);
    }
}
