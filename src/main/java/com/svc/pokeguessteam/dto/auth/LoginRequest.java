package com.svc.pokeguessteam.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "{error.validation.login.required}")
        @Email(message = "{error.validation.login.email.format}")
        @Size(max = 255, message = "{error.validation.login.size}")
        String email,
        @NotBlank(message = "{error.validation.password.required}")
        @Size(max = 72, message = "{error.validation.password.size.login}")
        String password
) {
}
