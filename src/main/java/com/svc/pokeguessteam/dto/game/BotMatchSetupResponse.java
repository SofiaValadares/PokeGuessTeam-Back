package com.svc.pokeguessteam.dto.game;

import java.util.List;

/** Commit público: C = SHA-256(team || nonce). O nonce fica selado no servidor até o finish. */
public record BotMatchSetupResponse(
        String matchId,
        List<Integer> hostTeam,
        List<Integer> opponentTeam,
        String hostCommitment,
        String opponentCommitment
) {
}
