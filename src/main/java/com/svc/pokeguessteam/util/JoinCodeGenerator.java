package com.svc.pokeguessteam.util;

import java.security.SecureRandom;
import java.util.function.Predicate;

public final class JoinCodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

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
        char[] buffer = new char[GameConstants.FRIEND_JOIN_CODE_LENGTH];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(buffer);
    }
}
