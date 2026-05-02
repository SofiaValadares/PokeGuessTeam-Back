package com.svc.pokeguessteam.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "{error.validation.auth.current-password.required}")
        String currentPassword,
        @NotBlank(message = "{error.validation.register.password.required}")
        @Size(min = 6, max = 72, message = "{error.validation.register.password.size}")
        String newPassword
) {
}
