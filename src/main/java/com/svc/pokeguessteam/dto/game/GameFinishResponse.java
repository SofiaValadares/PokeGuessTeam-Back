package com.svc.pokeguessteam.dto.game;

public record GameFinishResponse(
        GameHistoryEntryDto historyEntry,
        MatchRewardDto reward,
        String hostCommitment,
        String opponentCommitment,
        TeamOpeningDto hostOpening,
        TeamOpeningDto opponentOpening
) {
}
