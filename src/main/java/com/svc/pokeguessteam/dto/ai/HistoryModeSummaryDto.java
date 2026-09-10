package com.svc.pokeguessteam.dto.ai;

import com.svc.pokeguessteam.model.enums.GameModes;

public record HistoryModeSummaryDto(
        GameModes gameMode,
        int matches,
        int wins,
        int losses,
        int draws,
        int desistences,
        double winRate
) {
}