package com.svc.pokeguessteam.dto.user;

import com.svc.pokeguessteam.model.user.UserModel;

import java.time.LocalDateTime;

public record UserDto(
        String userId,
        String username,
        String email,
        boolean emailVerified,
        LocalDateTime registerDate,
        String role,
        boolean siteBanned,
        boolean onlineBanned
) {
    public static UserDto from(UserModel user) {
        return new UserDto(
                user.getIdUser(),
                user.getUsername(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerify()),
                user.getRegisterDate(),
                user.getRole().name(),
                user.isSiteBannedNow(),
                user.isOnlineBannedNow()
        );
    }
}
