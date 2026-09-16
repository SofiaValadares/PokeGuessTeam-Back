package com.svc.pokeguessteam.dto.game;

/** Commit público. O nonce permanece em AES-GCM no servidor até o open no finish. */
public record LocalMatchSetupResponse(
        String matchId,
        String hostCommitment,
        String opponentCommitment
) {
}
