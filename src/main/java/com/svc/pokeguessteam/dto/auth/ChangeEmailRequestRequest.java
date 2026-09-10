package com.svc.pokeguessteam.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequestRequest(
        @NotBlank(message = "{error.validation.register.email.required}")
        @Email(message = "{error.validation.register.email.format}")
        String newEmail,
        @NotBlank(message = "{error.validation.register.password.required}")
        String currentPassword
) {
}
