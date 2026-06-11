package com.svc.pokeguessteam.realtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchRealtimeMessage(
        MatchRealtimeEventType type,
        String matchId,
        FriendMatchStateDto friendMatch,
        BotMatchGuessFeedbackDto feedback,
        MatchPlayerSide currentTurn,
        LocalDateTime turnDeadlineAt,
        Integer turnTimeoutSeconds,
        String message
) {
    public static MatchRealtimeMessage friendPlayerGuess(
            String matchId,
            FriendMatchStateDto match,
            BotMatchGuessFeedbackDto feedback
    ) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.PLAYER_GUESS,
                matchId,
                match,
                feedback,
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
                match,
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
                match,
                null,
                currentTurn,
                deadlineAt,
                seconds,
                null
        );
    }

    public static MatchRealtimeMessage finishedFriend(String matchId, FriendMatchStateDto match) {
        return new MatchRealtimeMessage(
                MatchRealtimeEventType.MATCH_FINISHED,
                matchId,
                match,
                null,
                null,
                null,
                null,
                null
        );
    }
}
