package com.svc.pokeguessteam.security;

import com.svc.pokeguessteam.config.AppSecurityProperties;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mitiga força bruta no login: conta falhas por e-mail e por IP e aplica lockout temporário.
 * Contas inexistentes usam a mesma chave de e-mail para não revelar se o endereço existe.
 */
@Service
public class LoginAttemptService {

    private final AppSecurityProperties.Login settings;
    private final Clock clock;
    private final ConcurrentHashMap<String, AttemptState> byEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptState> byIp = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(AppSecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public LoginAttemptService(AppSecurityProperties properties, Clock clock) {
        this.settings = properties.getLogin();
        this.clock = clock;
    }

    public void assertNotLocked(String email, String ip) {
        Instant now = clock.instant();
        Instant until = later(
                lockExpiry(byEmail.get(normalizeEmail(email)), now),
                lockExpiry(byIp.get(normalizeIp(ip)), now)
        );
        if (until != null) {
            throwLocked(now, until);
        }
    }

    public void recordFailure(String email, String ip) {
        Instant now = clock.instant();
        bump(
                byEmail,
                normalizeEmail(email),
                now,
                settings.getMaxFailedAttempts(),
                settings.getLockoutMinutes()
        );
        bump(
                byIp,
                normalizeIp(ip),
                now,
                settings.getIpMaxFailedAttempts(),
                settings.getIpLockoutMinutes()
        );
    }

    public void recordSuccess(String email) {
        byEmail.remove(normalizeEmail(email));
    }

    private void bump(
            ConcurrentHashMap<String, AttemptState> map,
            String key,
            Instant now,
            int maxFailures,
            int lockoutMinutes
    ) {
        map.compute(key, (ignored, current) -> {
            AttemptState state = reusable(current, now);
            state.failures += 1;
            state.windowStart = state.windowStart == null ? now : state.windowStart;
            if (state.failures >= maxFailures) {
                state.lockedUntil = now.plus(Duration.ofMinutes(Math.max(1, lockoutMinutes)));
            }
            return state;
        });
    }

    private AttemptState reusable(AttemptState current, Instant now) {
        if (current == null) {
            return new AttemptState();
        }
        if (current.lockedUntil != null && current.lockedUntil.isAfter(now)) {
            return current;
        }
        if (current.lockedUntil != null) {
            return new AttemptState();
        }
        Duration window = Duration.ofMinutes(Math.max(1, settings.getWindowMinutes()));
        if (current.windowStart != null && current.windowStart.plus(window).isBefore(now)) {
            return new AttemptState();
        }
        return current;
    }

    private Instant lockExpiry(AttemptState state, Instant now) {
        if (state == null || state.lockedUntil == null || !state.lockedUntil.isAfter(now)) {
            return null;
        }
        return state.lockedUntil;
    }

    private static Instant later(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private static void throwLocked(Instant now, Instant until) {
        long seconds = Math.max(1, Duration.between(now, until).getSeconds());
        long minutes = Math.max(1, (seconds + 59) / 60);
        throw ApiBusinessException.withRetryAfter(
                HttpStatus.TOO_MANY_REQUESTS,
                ErrorCodes.AUTH_LOGIN_LOCKED,
                MessageKeys.AUTH_LOGIN_LOCKED,
                (int) seconds,
                minutes
        );
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.trim();
    }

    private static final class AttemptState {
        private int failures;
        private Instant windowStart;
        private Instant lockedUntil;
    }
}
