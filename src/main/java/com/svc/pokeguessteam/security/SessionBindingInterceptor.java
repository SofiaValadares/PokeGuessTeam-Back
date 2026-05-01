package com.svc.pokeguessteam.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionBindingInterceptor implements HandlerInterceptor {

    public static final String DEVICE_ID_ATTR = "DEVICE_ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return unauthorized(response, null, "Unauthorized.");
        }

        HttpSession session = request.getSession(false);

        if (session == null) {
            return unauthorized(response, null, "Session not found.");
        }

        Object expectedDeviceId = session.getAttribute(DEVICE_ID_ATTR);
        if (!(expectedDeviceId instanceof String storedDeviceId)) {
            return unauthorized(response, session, "Session binding not found.");
        }

        String currentDeviceId = DeviceFingerprintUtil.generateDeviceId(request);
        if (!storedDeviceId.equals(currentDeviceId)) {
            return unauthorized(response, session, "Session binding mismatch.");
        }

        return true;
    }

    private boolean unauthorized(HttpServletResponse response, HttpSession session, String message) throws Exception {
        if (session != null) {
            session.invalidate();
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        return false;
    }
}
