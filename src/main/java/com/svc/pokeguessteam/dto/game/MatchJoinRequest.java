package com.svc.pokeguessteam.dto.game;

import jakarta.validation.constraints.NotBlank;

public record MatchJoinRequest(
        @NotBlank String matchId,
        @NotBlank String mode
) {
}
