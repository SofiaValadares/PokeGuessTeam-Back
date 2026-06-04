package com.svc.pokeguessteam.realtime;

public enum MatchRealtimeEventType {
    /** Estado após palpite do jogador humano (ou timeout automático). */
    PLAYER_GUESS,
    /** UI deve bloquear: vez do bot. */
    BOT_TURN_START,
    /** Um palpite do bot (enviar um por mensagem). */
    BOT_GUESS,
    /** Atualização de estado sem palpite (fim de turno do bot, início de timer, etc.). */
    MATCH_STATE,
    /** Novo prazo de turno (modo amigo). */
    TURN_TIMER,
    /** Penalidade por timeout registada. */
    TIMEOUT_PENALTY,
    /** Adversário substituído por bot após 3 penalidades. */
    OPPONENT_REPLACED_BY_BOT,
    MATCH_FINISHED
}
