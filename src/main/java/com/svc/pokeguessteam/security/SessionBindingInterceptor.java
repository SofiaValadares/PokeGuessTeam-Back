package com.svc.pokeguessteam.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.svc.pokeguessteam.exception.ApiErrorResponse;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class SessionBindingInterceptor implements HandlerInterceptor {

    public static final String DEVICE_ID_ATTR = "DEVICE_ID";

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public SessionBindingInterceptor(ObjectMapper objectMapper, MessageSource messageSource) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Locale locale = LocaleContextHolder.getLocale();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return unauthorized(response, null, ErrorCodes.UNAUTHENTICATED,
                    MessageKeys.SESSION_INVALID_OR_EXPIRED, locale);
        }

        HttpSession session = request.getSession(false);

        if (session == null) {
            return unauthorized(response, null, ErrorCodes.SESSION_NOT_FOUND,
                    MessageKeys.SESSION_NONE_ACTIVE, locale);
        }

        Object expectedDeviceId = session.getAttribute(DEVICE_ID_ATTR);
        if (!(expectedDeviceId instanceof String storedDeviceId)) {
            return unauthorized(response, session, ErrorCodes.SESSION_BINDING_MISSING,
                    MessageKeys.SESSION_BINDING_MISSING, locale);
        }

        String currentDeviceId = DeviceFingerprintUtil.generateDeviceId(request);
        if (!storedDeviceId.equals(currentDeviceId)) {
            return unauthorized(response, session, ErrorCodes.SESSION_BINDING_MISMATCH,
                    MessageKeys.SESSION_BINDING_MISMATCH, locale);
        }

        return true;
    }

    private boolean unauthorized(HttpServletResponse response, HttpSession session, String code,
                                 String messageKey, Locale locale) throws Exception {
        if (session != null) {
            session.invalidate();
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = messageSource.getMessage(messageKey, null, messageKey, locale);
        ApiErrorResponse body = ApiErrorResponse.of(code, message);
        objectMapper.writeValue(response.getOutputStream(), body);
        return false;
    }
}
