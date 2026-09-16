package com.svc.pokeguessteam.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BonusEventUpsertRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @Min(1) Integer durationHours,
        @NotNull @DecimalMin("1.0") Double xpMultiplier,
        @NotEmpty List<Integer> pokedexNumbers
) {
}
