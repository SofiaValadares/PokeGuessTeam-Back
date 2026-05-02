package com.svc.pokeguessteam.dto.auth;

public record RegisterResponse(
        String userId,
        String email,
        String username
) { }
