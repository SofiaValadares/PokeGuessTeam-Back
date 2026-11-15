package com.svc.pokeguessteam.dto.admin;

import com.svc.pokeguessteam.model.admin.BonusEventModel;

import java.time.LocalDateTime;
import java.util.List;

public record ActiveBonusEventDto(
        String id,
        String name,
        String description,
        Double xpMultiplier,
        LocalDateTime startedAt,
        LocalDateTime endsAt,
        List<Integer> pokedexNumbers
) {
    public static ActiveBonusEventDto from(BonusEventModel event) {
        return new ActiveBonusEventDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getXpMultiplier(),
                event.getStartedAt(),
                event.getEndsAt(),
                List.copyOf(event.getPokedexNumbers())
        );
    }
}
