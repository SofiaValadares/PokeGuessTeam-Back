package com.svc.pokeguessteam.validation;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 72;
    public static final String REGEX = "^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{" + MIN_LENGTH + "," + MAX_LENGTH + "}$";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private PasswordPolicy() {
    }

    public static boolean matches(String password) {
        return password != null && PATTERN.matcher(password).matches();
    }
}
