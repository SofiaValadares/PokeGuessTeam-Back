package com.svc.pokeguessteam.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.svc.pokeguessteam.exception.ApiErrorResponse;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SessionIpBindingFilter extends OncePerRequestFilter {

    public static final String CLIENT_IP_ATTR = "CLIENT_IP";
    private static final String USER_ID_ATTR = "USER_ID";
    private static final Logger log = LoggerFactory.getLogger(SessionIpBindingFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String[] SKIP_PATTERNS = {
            "/auth/login",
            "/auth/register",
            "/auth/logout",
            "/auth/email/verification/send",
            "/auth/email/verification/confirm",
            "/auth/verification/resend",
            "/auth/verification/confirm",
            "/auth/password-reset/request",
            "/auth/password-reset/confirm",
            "/api/meta",
            "/public/**",
            "/error",
            "/favicon.ico"
    };

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public SessionIpBindingFilter(ObjectMapper objectMapper, MessageSource messageSource) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        for (String pattern : SKIP_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(USER_ID_ATTR) == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Object bound = session.getAttribute(CLIENT_IP_ATTR);
        if (!(bound instanceof String boundIp) || boundIp.isBlank()) {
            destroySession(session);
            writeJson(response, ErrorCodes.SESSION_BINDING_MISSING, MessageKeys.SESSION_BINDING_MISSING);
            return;
        }

        String currentIp = ClientIpResolver.resolve(request);
        if (!ClientIpResolver.sameClient(boundIp, currentIp)) {
            log.warn(
                    "Sessão invalidada por IP divergente. bound={} current={}",
                    boundIp,
                    currentIp
            );
            destroySession(session);
            writeJson(response, ErrorCodes.SESSION_IP_MISMATCH, MessageKeys.SESSION_IP_MISMATCH);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static void destroySession(HttpSession session) {
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // já invalidada
        }
        SecurityContextHolder.clearContext();
    }

    private void writeJson(HttpServletResponse response, String code, String messageKey) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = messageSource.getMessage(
                messageKey,
                null,
                messageKey,
                LocaleContextHolder.getLocale()
        );
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(code, message));
    }
}
