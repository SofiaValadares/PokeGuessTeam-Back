package com.svc.pokeguessteam.dto.dev;

import jakarta.validation.constraints.NotNull;

public record AdjustTrainingTeamXpRequest(
        @NotNull Integer delta
) {
}
