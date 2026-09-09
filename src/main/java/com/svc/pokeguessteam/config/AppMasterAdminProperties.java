package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Usernames do master admin (separados por vírgula em {@code MASTER_ADMIN_USERNAMES}).
 * Não commitar valores reais — só no {@code .env} local / secrets do host.
 */
@ConfigurationProperties(prefix = "app.master-admin")
public class AppMasterAdminProperties {

    /** CSV: {@code aisso} ou {@code aisso,outro}. */
    private String usernames = "";

    public String getUsernames() {
        return usernames;
    }

    public void setUsernames(String usernames) {
        this.usernames = usernames != null ? usernames.trim() : "";
    }

    public boolean isMasterUsername(String username) {
        if (username == null || username.isBlank() || usernames.isBlank()) {
            return false;
        }
        String needle = username.trim().toLowerCase(Locale.ROOT);
        return parsedUsernames().stream().anyMatch(needle::equals);
    }

    private List<String> parsedUsernames() {
        if (usernames == null || usernames.isBlank()) {
            return List.of();
        }
        return Arrays.stream(usernames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
