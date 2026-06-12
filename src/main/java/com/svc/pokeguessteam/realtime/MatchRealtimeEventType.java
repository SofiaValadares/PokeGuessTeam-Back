package com.svc.pokeguessteam.realtime;

public enum MatchRealtimeEventType {
    /** Estado após palpite do jogador humano (ou timeout automático). */
    PLAYER_GUESS,
    /** Atualização de estado sem palpite (fim de turno do bot, início de timer, etc.). */
    MATCH_STATE,
    /** Novo prazo de turno (modo amigo). */
    TURN_TIMER,
    MATCH_FINISHED
}
