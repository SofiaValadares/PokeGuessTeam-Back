package com.svc.pokeguessteam.dto.auth;

import com.svc.pokeguessteam.validation.StrongPassword;
import com.svc.pokeguessteam.validation.UsernameDistinctFromEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@UsernameDistinctFromEmail
public record RegisterRequest(
        @NotBlank(message = "{error.validation.register.username.required}")
        @Size(max = 100, message = "{error.validation.register.username.size}")
        String username,
        @NotBlank(message = "{error.validation.register.email.required}")
        @Email(message = "{error.validation.register.email.format}")
        String email,
        @NotBlank(message = "{error.validation.register.password.required}")
        @StrongPassword
        String password
) {
}
