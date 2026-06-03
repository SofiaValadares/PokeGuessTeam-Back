package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.util.GameConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BotMatchTeamRequest(
        @NotNull
        @Size(min = GameConstants.TEAM_SIZE, max = GameConstants.TEAM_SIZE)
        List<Integer> team
) {
}
