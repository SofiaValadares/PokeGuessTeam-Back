package com.svc.pokeguessteam.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.svc.pokeguessteam.exception.ApiErrorResponse;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper, MessageSource messageSource) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                MessageKeys.SECURITY_AUTHENTICATION_REQUIRED,
                null,
                MessageKeys.SECURITY_AUTHENTICATION_REQUIRED,
                locale
        );
        ApiErrorResponse body = ApiErrorResponse.of(ErrorCodes.UNAUTHENTICATED, message);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
