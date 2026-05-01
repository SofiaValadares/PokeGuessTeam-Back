package com.svc.pokeguessteam.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateMatchRequest(
        @NotEmpty
        @Size(min = 6, max = 6)
        List<String> playerTeamPokemonIds
) {
}
