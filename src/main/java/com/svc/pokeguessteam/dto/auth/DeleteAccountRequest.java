package com.svc.pokeguessteam.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "{error.validation.register.password.required}")
        String password
) {
}
