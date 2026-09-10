package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.game.HistoryGameModel;
import org.springframework.data.domain.Page;

import java.util.List;

public record GameHistoryPageResponse(
        List<GameHistoryEntryDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static GameHistoryPageResponse from(Page<HistoryGameModel> page) {
        List<GameHistoryEntryDto> items = page.getContent().stream()
                .map(GameHistoryEntryDto::from)
                .toList();
        return new GameHistoryPageResponse(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
