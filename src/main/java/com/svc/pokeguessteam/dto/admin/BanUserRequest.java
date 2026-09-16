package com.svc.pokeguessteam.dto.admin;

import com.svc.pokeguessteam.model.enums.BanScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BanUserRequest(
        @NotNull BanScope scope,
        boolean permanent,
        Integer durationHours,
        @NotBlank @Size(max = 500) String reason
) {
}
