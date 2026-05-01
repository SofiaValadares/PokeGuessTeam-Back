package com.svc.pokeguessteam.dto.auth;

import java.util.Optional;

public record SessionResponse(
        Boolean authenticated,
        Optional<String> userId
) {
}
