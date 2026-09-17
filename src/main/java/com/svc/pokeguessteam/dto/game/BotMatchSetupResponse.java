package com.svc.pokeguessteam.dto.game;

/** Commit público: C = SHA-256(team || nonce). O time do bot permanece selado no servidor. */
public record BotMatchSetupResponse(
        String matchId,
        java.util.List<Integer> hostTeam,
        String hostCommitment,
        String opponentCommitment
) {
}
