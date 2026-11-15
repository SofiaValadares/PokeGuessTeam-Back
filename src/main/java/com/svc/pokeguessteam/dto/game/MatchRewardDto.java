package com.svc.pokeguessteam.dto.game;

public record MatchRewardDto(
        int trainingTeamXpGranted,
        int pokeBallsGranted,
        int pokeballFragmentsGranted,
        /** Tipo da bola concedida (ex.: {@code FRIEND_BALL}); null se não houve bolas. */
        String pokeballTypeGranted
) {
    public static MatchRewardDto of(int xp, int balls, int fragments, String ballType) {
        return new MatchRewardDto(xp, balls, fragments, balls > 0 ? ballType : null);
    }

    public static MatchRewardDto empty() {
        return new MatchRewardDto(0, 0, 0, null);
    }
}
