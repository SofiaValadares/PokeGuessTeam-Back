package com.svc.pokeguessteam.dto.game;

import java.util.List;

public record FriendMatchActionResponse(
        FriendMatchStateDto match,
        List<BotMatchGuessFeedbackDto> turnFeedbacks
) {
}
