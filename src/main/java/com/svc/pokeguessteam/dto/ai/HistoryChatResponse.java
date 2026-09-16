package com.svc.pokeguessteam.dto.ai;

public record HistoryChatResponse(
        String answer,
        String intent,
        boolean aiUsed,
        HistorySummaryDto summary
) {
}