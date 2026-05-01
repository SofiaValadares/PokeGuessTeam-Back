package com.svc.pokeguessteam.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(
        @NotBlank
        String token
) {
}
