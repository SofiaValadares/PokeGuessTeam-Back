package com.svc.pokeguessteam.security;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite por IP para cadastro / reenvio de código / reset — in-memory, alinhado ao lockout de login.
 */
@Service
public class AuthIpRateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void consume(String bucket, String ip, int maxHits, int windowMinutes) {
        String key = bucket + "|" + (ip == null || ip.isBlank() ? "unknown" : ip.trim());
        Instant now = Instant.now();
        Duration window = Duration.ofMinutes(Math.max(1, windowMinutes));
        Window state = windows.compute(key, (ignored, current) -> {
            Window next = current == null || current.startedAt.plus(window).isBefore(now)
                    ? new Window()
                    : current;
            next.hits += 1;
            if (next.startedAt == null) {
                next.startedAt = now;
            }
            if (next.hits > maxHits) {
                next.blockedUntil = next.startedAt.plus(window);
            }
            return next;
        });
        if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
            long seconds = Math.max(1, Duration.between(now, state.blockedUntil).getSeconds());
            long minutes = Math.max(1, (seconds + 59) / 60);
            throw ApiBusinessException.withRetryAfter(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCodes.AUTH_RATE_LIMITED,
                    MessageKeys.AUTH_RATE_LIMITED,
                    (int) seconds,
                    minutes
            );
        }
    }

    private static final class Window {
        private Instant startedAt;
        private Instant blockedUntil;
        private int hits;
    }
}
