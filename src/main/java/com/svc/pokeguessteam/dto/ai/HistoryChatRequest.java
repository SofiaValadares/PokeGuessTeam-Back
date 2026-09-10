package com.svc.pokeguessteam.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record HistoryChatRequest(
        @NotBlank String message
) {
}