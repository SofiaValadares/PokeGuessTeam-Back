package com.svc.pokeguessteam.dto.ai;

import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.model.enums.GameModes;

import java.util.List;

public record HistorySummaryDto(
        int totalMatches,
        int wins,
        int losses,
        int draws,
        int desistences,
        double winRate,
        double averageCorrectGuesses,
        String mostUsedPokemonName,
        Integer mostUsedPokemonDex,
        int mostUsedPokemonCount,
        GameModes bestMode,
        List<HistoryModeSummaryDto> modeSummaries,
        List<GameHistoryEntryDto> recentMatches
) {
}