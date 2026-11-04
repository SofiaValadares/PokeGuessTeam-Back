package com.svc.pokeguessteam.dto.pokemon;

import com.svc.pokeguessteam.model.enums.PokeballType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PokeballDrawRequest(
        @NotNull PokeballType pokeballType,
        /** Quantidade de capturas (1–10). Default 1 se omitido. */
        @Min(1) @Max(10) Integer count
) {
    public int resolvedCount() {
        return count == null ? 1 : count;
    }
}
