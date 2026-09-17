package com.svc.pokeguessteam.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.svc.pokeguessteam.exception.ApiErrorResponse;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.repository.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class DatabaseRoleRefreshFilter extends OncePerRequestFilter {

    private static final String USER_ID_ATTR = "USER_ID";

    private final UserRepository userRepository;
    private final SessionAuthorityService sessionAuthorityService;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public DatabaseRoleRefreshFilter(
            UserRepository userRepository,
            SessionAuthorityService sessionAuthorityService,
            ObjectMapper objectMapper,
            MessageSource messageSource
    ) {
        this.userRepository = userRepository;
        this.sessionAuthorityService = sessionAuthorityService;
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            Object userId = session.getAttribute(USER_ID_ATTR);

            if (userId != null) {
                var found = userRepository.findById(userId.toString());
                if (found.isEmpty()) {
                    session.invalidate();
                    SecurityContextHolder.clearContext();
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED,
                            MessageKeys.SESSION_INVALID_OR_EXPIRED);
                    return;
                }
                var user = found.get();
                if (user.isSiteBannedNow()) {
                    session.invalidate();
                    SecurityContextHolder.clearContext();
                    writeJson(response, HttpServletResponse.SC_FORBIDDEN, ErrorCodes.AUTH_USER_SITE_BANNED,
                            MessageKeys.AUTH_USER_SITE_BANNED);
                    return;
                }
                sessionAuthorityService.applyToSession(user, session);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeJson(HttpServletResponse response, int status, String code, String messageKey) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = messageSource.getMessage(messageKey, null, messageKey, LocaleContextHolder.getLocale());
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(code, message));
    }
}
