package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.util.GameConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TeamOpeningDto(
        @NotNull
        @Size(min = GameConstants.TEAM_SIZE, max = GameConstants.TEAM_SIZE)
        List<Integer> team,
        @NotBlank
        @Pattern(regexp = "[0-9a-fA-F]{64}")
        String nonce
) {
}
