package com.svc.pokeguessteam.dto.user;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.svc.pokeguessteam.model.user.UserModel;
import org.springframework.security.core.Authentication;

public record MeResponse(
        @JsonUnwrapped
        UserDto user,
        String authenticatedAs
) {
    public static MeResponse from(Authentication authentication, UserModel user) {
        return new MeResponse(UserDto.from(user), authentication.getName());
    }
}
