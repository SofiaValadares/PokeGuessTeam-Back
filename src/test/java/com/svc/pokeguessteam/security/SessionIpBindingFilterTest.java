package com.svc.pokeguessteam.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.svc.pokeguessteam.exception.ErrorCodes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionIpBindingFilterTest {

    private SessionIpBindingFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("error.session.ip-mismatch", Locale.getDefault(), "Sua sessão foi encerrada por motivos de segurança.");
        messages.addMessage("error.session.binding-missing", Locale.getDefault(), "Sessão sem IP vinculado.");
        filter = new SessionIpBindingFilter(objectMapper, messages);
        LocaleContextHolder.setLocale(Locale.getDefault());
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsMatchingIpAndKeepsSession() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("203.0.113.10", "203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(request.getSession(false));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidatesSessionAndReturns401WhenIpDiffers() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("203.0.113.10", "198.51.100.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(ErrorCodes.SESSION_IP_MISMATCH, body.get("code").asText());
        assertTrue(body.get("message").asText().toLowerCase().contains("segurança"));
        assertNull(request.getSession(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void ignoresSpoofedForwardedForFromPublicClient() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("203.0.113.10", "198.51.100.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(ErrorCodes.SESSION_IP_MISMATCH, body.get("code").asText());
        assertNull(request.getSession(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void skipsLoginWithoutCheckingIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath("/auth/login");
        request.setRemoteAddr("198.51.100.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsByteArray().length == 0);
    }

    private MockHttpServletRequest authenticatedRequest(String boundIp, String currentIp) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/me");
        request.setRemoteAddr(currentIp);
        HttpSession session = request.getSession(true);
        session.setAttribute("USER_ID", "user-1");
        session.setAttribute(SessionIpBindingFilter.CLIENT_IP_ATTR, boundIp);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null, List.of())
        );
        return request;
    }
}
