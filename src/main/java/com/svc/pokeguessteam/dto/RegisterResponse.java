package com.svc.pokeguessteam.dto;

import com.svc.pokeguessteam.model.UserModel;

public record RegisterResponse(
        String userId,
        String email,
        String username
) { }
