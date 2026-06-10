package com.svc.pokeguessteam.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

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

    public static String generateUnique(Predicate<String> isTaken) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomCode();
            if (!isTaken.test(code)) {
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
