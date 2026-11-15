package com.svc.pokeguessteam.dto.admin;

public record SystemLogEntryDto(
        String timestamp,
        String level,
        String origin,
        String message,
        String raw
) {
}
