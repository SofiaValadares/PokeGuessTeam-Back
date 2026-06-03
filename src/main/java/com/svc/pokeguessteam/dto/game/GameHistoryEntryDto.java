package com.svc.pokeguessteam.dto.game;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.game.HistoryGameModel;
import com.svc.pokeguessteam.model.game.HistoryGamePlayerModel;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record GameHistoryEntryDto(
        String id,
        GameModes gameMode,
        LocalDateTime playedAt,
        String opponentName,
        List<GameHistoryPlayerDto> players
) {
    public static GameHistoryEntryDto from(HistoryGameModel game) {
        List<GameHistoryPlayerDto> players = game.getPlayers().stream()
                .sorted(Comparator.comparing(HistoryGamePlayerModel::getSlot))
                .map(GameHistoryPlayerDto::from)
                .toList();
        return new GameHistoryEntryDto(
                game.getId(),
                game.getGameMode(),
                game.getPlayedAt(),
                game.getOpponentName(),
                players
        );
    }
}
