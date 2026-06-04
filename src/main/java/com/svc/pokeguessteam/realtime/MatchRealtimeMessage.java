package com.svc.pokeguessteam.realtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.BotMatchStateDto;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchRealtimeMessage(
        MatchRealtimeEventType type,
        String matchId,
        BotMatchStateDto botMatch,
        FriendMatchStateDto friendMatch,
        BotMatchGuessFeedbackDto feedback,
        MatchPlayerSide currentTurn,
        LocalDateTime turnDeadlineAt,
        Integer turnTimeoutSeconds,
        Integer timeoutPenalties,
        Integer maxTimeoutPenalties,
        String message
) {
    public static MatchRealtimeMessage botPlayerGuess(String matchId, BotMatchStateDto match, BotMatchGuessFeedbackDto feedback) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.PLAYER_GUESS,
                matchId,
                match,
                null,
                feedback,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MatchRealtimeMessage botTurnStart(String matchId) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.BOT_TURN_START,
                matchId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Aguardando palpite do bot."
        );
    }

    public static MatchRealtimeMessage botGuess(String matchId, BotMatchStateDto match, BotMatchGuessFeedbackDto feedback) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.BOT_GUESS,
                matchId,
                match,
                null,
                feedback,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MatchRealtimeMessage botState(String matchId, BotMatchStateDto match) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.MATCH_STATE,
                matchId,
                match,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MatchRealtimeMessage friendPlayerGuess(
            String matchId,
            FriendMatchStateDto match,
            BotMatchGuessFeedbackDto feedback
    ) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.PLAYER_GUESS,
                matchId,
                null,
                match,
                feedback,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MatchRealtimeMessage friendState(String matchId, FriendMatchStateDto match) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.MATCH_STATE,
                matchId,
                null,
                match,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MatchRealtimeMessage friendTimer(
            String matchId,
            FriendMatchStateDto match,
            MatchPlayerSide currentTurn,
            LocalDateTime deadlineAt,
            int seconds
    ) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.TURN_TIMER,
                matchId,
                null,
                match,
                null,
                currentTurn,
                deadlineAt,
                seconds,
                null,
                null,
                null
        );
    }

    public static MatchRealtimeMessage timeoutPenalty(
            String matchId,
            FriendMatchStateDto match,
            int penalties,
            int maxPenalties
    ) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.TIMEOUT_PENALTY,
                matchId,
                null,
                match,
                null,
                null,
                null,
                null,
                penalties,
                maxPenalties,
                "Palpite automático por tempo esgotado."
        );
    }

    public static MatchRealtimeMessage opponentReplacedByBot(String matchId, FriendMatchStateDto match) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.OPPONENT_REPLACED_BY_BOT,
                matchId,
                null,
                match,
                null,
                null,
                null,
                null,
                null,
                null,
                "Adversário substituído por bot após 3 penalidades."
        );
    }

    public static MatchRealtimeMessage finishedBot(String matchId, BotMatchStateDto match) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.MATCH_FINISHED,
                matchId,
                match,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MatchRealtimeMessage finishedFriend(String matchId, FriendMatchStateDto match) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.MATCH_FINISHED,
                matchId,
                null,
                match,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
