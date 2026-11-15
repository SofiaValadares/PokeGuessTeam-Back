package com.svc.pokeguessteam.dto.auth;

import com.svc.pokeguessteam.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "{error.validation.auth.current-password.required}")
        String currentPassword,
        @NotBlank(message = "{error.validation.register.password.required}")
        @StrongPassword
        String newPassword
) {
}
