package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.user.TrainingTeamModel;

/**
 * Regras partilhadas com a beta (equipa de 6, palpites corretos = acertos no time adversário).
 */
public final class GameConstants {

    public static final int TEAM_SIZE = TrainingTeamModel.TEAM_SIZE;
    public static final int MAX_CORRECT_GUESSES = TEAM_SIZE;
    public static final int LOCAL_OPPONENT_NAME_MIN_LENGTH = 3;
    public static final int OPPONENT_NAME_MAX_LENGTH = 120;
    public static final int FRIEND_JOIN_CODE_LENGTH = 6;

    /** Tempo por turno no modo amigo (segundos). */
    public static final int FRIEND_TURN_TIMEOUT_SECONDS = 50;

    /** Penalidades por timeout na mesma partida → desistência automática. */
    public static final int FRIEND_MAX_TIMEOUT_PENALTIES_PER_MATCH = 3;

    /** Penalidades na última hora → ban temporário do modo amigo. */
    public static final int FRIEND_TIMEOUT_BAN_THRESHOLD = 5;

    public static final int FRIEND_TIMEOUT_BAN_HOURS = 72;

    /** Penalidades visíveis no perfil (dias). */
    public static final int FRIEND_PENALTY_PROFILE_RETENTION_DAYS = 7;

    /** Atraso entre palpites do bot enviados por WebSocket (ms). */
    public static final long BOT_GUESS_WS_DELAY_MS = 600L;

    private GameConstants() {
    }
}
