package com.svc.pokeguessteam.dto.game;

import java.util.List;

public record BotMatchSetupResponse(
        List<Integer> hostTeam,
        List<Integer> opponentTeam
) {
}
