package com.svc.pokeguessteam.validation;

import java.util.Locale;

public final class UsernameEmailGuard {

    private static final int MIN_FRAGMENT_LENGTH = 3;

    private UsernameEmailGuard() {
    }

    public static boolean conflicts(String username, String email) {
        if (username == null || email == null) {
            return false;
        }
        String user = username.trim().toLowerCase(Locale.ROOT);
        String mail = email.trim().toLowerCase(Locale.ROOT);
        if (user.isEmpty() || mail.isEmpty()) {
            return false;
        }
        if (user.equals(mail) || user.contains(mail)) {
            return true;
        }
        int at = mail.indexOf('@');
        String local = at > 0 ? mail.substring(0, at) : mail;
        if (local.length() >= MIN_FRAGMENT_LENGTH && user.contains(local)) {
            return true;
        }
        String localCompact = local.replaceAll("[^a-z0-9]", "");
        String userCompact = user.replaceAll("[^a-z0-9]", "");
        return localCompact.length() >= MIN_FRAGMENT_LENGTH && userCompact.contains(localCompact);
    }
}
