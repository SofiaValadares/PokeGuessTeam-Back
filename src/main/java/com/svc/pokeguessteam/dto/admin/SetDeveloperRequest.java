package com.svc.pokeguessteam.dto.admin;

import jakarta.validation.constraints.NotNull;

public record SetDeveloperRequest(@NotNull Boolean developer) {
}
