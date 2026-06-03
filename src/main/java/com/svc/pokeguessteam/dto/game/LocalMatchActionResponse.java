package com.svc.pokeguessteam.dto.game;

import java.util.List;

public record LocalMatchActionResponse(
        LocalMatchStateDto match,
        List<BotMatchGuessFeedbackDto> turnFeedbacks
) {
}
