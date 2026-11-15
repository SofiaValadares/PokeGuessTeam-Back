package com.svc.pokeguessteam.model.enums;

/**
 * Resultado na perspetiva do jogador autenticado (slot 1).
 * Resultado da partida na perspetiva do jogador autenticado.
 */
public enum GameResults {
    WIN,
    LOSE,
    DRAW,
    /** Jogador autenticado desistiu da partida. */
    DESISTENCE,
    /** Partida anulada por uma análise de anomalia com confiança suficiente. */
    CANCELLED
}
