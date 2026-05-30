package com.svc.pokeguessteam.dto.game;

import java.util.List;

public record BotMatchActionResponse(
        BotMatchStateDto match,
        List<BotMatchGuessFeedbackDto> turnFeedbacks
) {
}
