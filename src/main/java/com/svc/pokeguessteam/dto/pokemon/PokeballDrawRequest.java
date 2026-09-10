package com.svc.pokeguessteam.dto.pokemon;

import com.svc.pokeguessteam.model.enums.PokeballType;
import jakarta.validation.constraints.NotNull;

public record PokeballDrawRequest(
        @NotNull PokeballType pokeballType
) {
}
