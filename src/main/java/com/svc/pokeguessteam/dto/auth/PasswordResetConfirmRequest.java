package com.svc.pokeguessteam.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "{error.validation.register.email.required}")
        @Email(message = "{error.validation.register.email.format}")
        String email,

        @NotBlank(message = "{error.validation.auth.code.required}")
        @Pattern(regexp = "\\d{8}", message = "{error.validation.auth.code.format}")
        String code,

        @NotBlank(message = "{error.validation.register.password.required}")
        @Size(min = 6, max = 72, message = "{error.validation.register.password.size}")
        String newPassword
) {
}
