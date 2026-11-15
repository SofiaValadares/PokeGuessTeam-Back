package com.svc.pokeguessteam.dto.admin;

import java.util.List;

public record SystemLogListResponse(
        List<SystemLogEntryDto> entries,
        int returned,
        boolean truncated
) {
}
