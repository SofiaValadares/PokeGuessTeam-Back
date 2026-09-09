package com.svc.pokeguessteam.dto.admin;

import com.svc.pokeguessteam.model.admin.BonusEventModel;

import java.time.LocalDateTime;
import java.util.List;

public record BonusEventDto(
        String id,
        String name,
        String description,
        Integer durationHours,
        Double xpMultiplier,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endsAt,
        LocalDateTime createdAt,
        List<Integer> pokedexNumbers
) {
    public static BonusEventDto from(BonusEventModel event) {
        return new BonusEventDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getDurationHours(),
                event.getXpMultiplier(),
                event.getStatus().name(),
                event.getStartedAt(),
                event.getEndsAt(),
                event.getCreatedAt(),
                List.copyOf(event.getPokedexNumbers())
        );
    }
}
