package com.svc.pokeguessteam.dto.pokemon;

import java.util.List;

public record PokeballMultiDrawResponse(
        List<PokeballDrawResponse> draws
) {
}
