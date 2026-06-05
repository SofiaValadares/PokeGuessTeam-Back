package com.svc.pokeguessteam.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeEmailConfirmRequest(
        @NotBlank(message = "{error.validation.register.email.required}")
        @Email(message = "{error.validation.register.email.format}")
        String newEmail,
        @NotBlank(message = "{error.validation.auth.code.required}")
        @Pattern(regexp = "\\d{8}", message = "{error.validation.auth.code.format}")
        String code,
        @NotBlank(message = "{error.validation.register.password.required}")
        String currentPassword
) {
}
