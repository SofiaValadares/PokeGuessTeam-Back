package com.svc.pokeguessteam.dto.pokemon;

import java.util.Map;

public record ClaimEvolutionRewardsResponse(
        PcLineDto line,
        Map<String, Integer> grantedPokeballs
) {
}
