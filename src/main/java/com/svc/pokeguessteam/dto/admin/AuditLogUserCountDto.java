package com.svc.pokeguessteam.dto.admin;

public record AuditLogUserCountDto(
        String userId,
        String username,
        String email,
        long logCount
) {
}
