package com.svc.pokeguessteam.dto.admin;

import com.svc.pokeguessteam.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record SetUserRoleRequest(
        @NotNull UserRole role
) {
}
