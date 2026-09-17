package com.svc.pokeguessteam.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Índice de sessões HTTP por userId para invalidação imediata (banimento de site).
 */
@Component
public class ActiveUserSessionIndex {

    public static final String TRACKED_USER_ID_ATTR = "TRACKED_USER_ID";

    private final ConcurrentHashMap<String, Set<String>> sessionIdsByUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HttpSession> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userIdBySessionId = new ConcurrentHashMap<>();

    public void register(String userId, HttpSession session) {
        if (userId == null || session == null) {
            return;
        }
        String sessionId = session.getId();
        session.setAttribute(TRACKED_USER_ID_ATTR, userId);
        userIdBySessionId.put(sessionId, userId);
        sessionsById.put(sessionId, session);
        sessionIdsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void unregister(HttpSession session) {
        if (session == null) {
            return;
        }
        unregister(session.getId());
    }

    public void unregister(String sessionId) {
        if (sessionId == null) {
            return;
        }
        sessionsById.remove(sessionId);
        String userId = userIdBySessionId.remove(sessionId);
        if (userId == null) {
            return;
        }
        Set<String> ids = sessionIdsByUser.get(userId);
        if (ids != null) {
            ids.remove(sessionId);
            if (ids.isEmpty()) {
                sessionIdsByUser.remove(userId, ids);
            }
        }
    }

    public void invalidateAll(String userId) {
        if (userId == null) {
            return;
        }
        Set<String> ids = sessionIdsByUser.remove(userId);
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String sessionId : ids) {
            userIdBySessionId.remove(sessionId);
            HttpSession session = sessionsById.remove(sessionId);
            if (session == null) {
                continue;
            }
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // já invalidada
            }
        }
    }
}
