package com.svc.pokeguessteam.dto.auth;

import com.svc.pokeguessteam.model.UserModel;

public record RegisterResponse(
        String userId,
        String email,
        String username
) { }
