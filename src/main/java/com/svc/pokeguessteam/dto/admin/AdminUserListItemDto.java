package com.svc.pokeguessteam.dto.admin;

import com.svc.pokeguessteam.model.user.UserModel;

import java.time.LocalDateTime;

public record AdminUserListItemDto(
        String userId,
        String username,
        String email,
        String role,
        boolean siteBanned,
        boolean onlineBanned,
        long abandonedMatchesCount,
        LocalDateTime registerDate
) {
    public static AdminUserListItemDto from(UserModel user, long abandonedMatchesCount) {
        return new AdminUserListItemDto(
                user.getIdUser(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isSiteBannedNow(),
                user.isOnlineBannedNow(),
                abandonedMatchesCount,
                user.getRegisterDate()
        );
    }
}
