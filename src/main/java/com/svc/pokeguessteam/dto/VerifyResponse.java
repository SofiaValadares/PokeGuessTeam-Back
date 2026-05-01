package com.svc.pokeguessteam.dto;

public record VerifyResponse(
        String userId,
        String email,
        String username,
        String message
) {
}
