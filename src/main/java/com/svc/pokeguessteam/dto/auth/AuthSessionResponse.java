package com.svc.pokeguessteam.dto.auth;

public record AuthSessionResponse(
        String userId,
        String email,
        String username,
        boolean emailVerified,
        String message,
        boolean firstLogin
) {
}
