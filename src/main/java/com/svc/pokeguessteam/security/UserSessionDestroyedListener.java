package com.svc.pokeguessteam.security;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

@Component
public class UserSessionDestroyedListener implements HttpSessionListener {

    private final ActiveUserSessionIndex activeUserSessionIndex;

    public UserSessionDestroyedListener(ActiveUserSessionIndex activeUserSessionIndex) {
        this.activeUserSessionIndex = activeUserSessionIndex;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        activeUserSessionIndex.unregister(se.getSession());
    }
}
