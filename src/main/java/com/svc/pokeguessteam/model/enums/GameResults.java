package com.svc.pokeguessteam.model.enums;

/**
 * Resultado na perspetiva do jogador autenticado (slot 1).
 * Alinhado à beta: vitória, derrota, empate e desistência.
 */
public enum GameResults {
    WIN,
    LOSE,
    DRAW,
    /** Jogador autenticado desistiu da partida. */
    DESISTENCE
}
