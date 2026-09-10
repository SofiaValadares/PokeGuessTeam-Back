package com.svc.pokeguessteam.dto.game;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FriendMatchActionResponse(
        FriendMatchStateDto match,
        List<BotMatchGuessFeedbackDto> turnFeedbacks,
        MatchRewardDto reward
) {
    public FriendMatchActionResponse(
            FriendMatchStateDto match,
            List<BotMatchGuessFeedbackDto> turnFeedbacks
    ) {
        this(match, turnFeedbacks, null);
    }
}
