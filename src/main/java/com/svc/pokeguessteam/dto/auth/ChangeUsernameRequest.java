package com.svc.pokeguessteam.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequest(
        @NotBlank(message = "{error.validation.register.username.required}")
        @Size(max = 100, message = "{error.validation.register.username.size}")
        String newUsername,
        @NotBlank(message = "{error.validation.register.password.required}")
        String password
) {
}
