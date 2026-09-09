package com.svc.pokeguessteam.dto.admin;

import com.svc.pokeguessteam.model.enums.BanScope;
import jakarta.validation.constraints.NotNull;

public record UnbanUserRequest(
        @NotNull BanScope scope
) {
}
