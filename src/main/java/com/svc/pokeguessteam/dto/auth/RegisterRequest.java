package com.svc.pokeguessteam.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "{error.validation.register.username.required}")
        @Size(max = 100, message = "{error.validation.register.username.size}")
        String username,
        @NotBlank(message = "{error.validation.register.email.required}")
        @Email(message = "{error.validation.register.email.format}")
        String email,
        @NotBlank(message = "{error.validation.register.password.required}")
        @Size(min = 6, max = 72, message = "{error.validation.register.password.size}")
        String password
) {
}
