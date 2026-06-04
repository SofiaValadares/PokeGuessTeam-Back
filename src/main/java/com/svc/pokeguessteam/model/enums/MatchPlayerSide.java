package com.svc.pokeguessteam.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Lado numa partida ativa.
 * <ul>
 *   <li>{@link #HOST} — conta logada (modo bot/local) ou anfitrião (amigo)</li>
 *   <li>{@link #OPPONENT} — IA (bot), 2.º jogador no mesmo dispositivo (local) ou convidado (amigo)</li>
 * </ul>
 * Aceita aliases legados {@code USER} e {@code BOT} em JSON e na base de dados.
 */
public enum MatchPlayerSide {
    HOST,
    OPPONENT;

    @JsonCreator
    public static MatchPlayerSide fromJson(String value) {
        return fromLegacy(value);
    }

    public static MatchPlayerSide fromLegacy(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "USER", "HOST", "PLAYER_ONE", "LOCAL_HOST" -> HOST;
            case "BOT", "OPPONENT", "PLAYER_TWO", "LOCAL_OPPONENT", "GUEST" -> OPPONENT;
            default -> valueOf(value.trim().toUpperCase());
        };
    }
}
