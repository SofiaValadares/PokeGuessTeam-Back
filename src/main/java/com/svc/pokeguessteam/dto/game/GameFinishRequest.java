package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.GameResults;

/**
 * Campos comuns ao terminar uma partida (perspetiva do utilizador autenticado).
 */
public interface GameFinishRequest {

    int userCorrectGuesses();

    int opponentCorrectGuesses();

    GameResults result();
}
