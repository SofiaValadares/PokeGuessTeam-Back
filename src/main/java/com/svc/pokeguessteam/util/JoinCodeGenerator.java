package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;

import java.util.concurrent.ThreadLocalRandom;

public final class JoinCodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private JoinCodeGenerator() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.trim().toUpperCase();
    }

    public static String generateUnique(ActiveMatchRepository repository) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomCode();
            boolean taken = repository.findByJoinCodeAndGameModeAndStatusNot(
                    code,
                    GameModes.FRIEND,
                    MatchStatus.FINISHED
            ).isPresent();
            if (!taken) {
                return code;
            }
        }
        throw new IllegalStateException("Não foi possível gerar código de convite único.");
    }

    private static String randomCode() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char[] buffer = new char[GameConstants.FRIEND_JOIN_CODE_LENGTH];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(buffer);
    }
}
