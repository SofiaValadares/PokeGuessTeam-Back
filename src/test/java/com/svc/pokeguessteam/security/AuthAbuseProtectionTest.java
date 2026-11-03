package com.svc.pokeguessteam.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.svc.pokeguessteam.config.AppAuthProperties;
import com.svc.pokeguessteam.config.AppSecurityProperties;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ApiErrorResponse;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.exception.GlobalExceptionHandler;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.auth.AuthCodePurpose;
import com.svc.pokeguessteam.service.AuthCodeHasher;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abuso de autenticação: lockout, rate limit, IP, HTTPS, sessão e códigos.
 */
class AuthAbuseProtectionTest {

    @Nested
    class LoginLockout {

        private static final String EMAIL = "ash@example.com";
        private static final String IP = "203.0.113.10";

        private AdjustableClock clock;
        private LoginAttemptService service;

        @BeforeEach
        void setUp() {
            clock = new AdjustableClock(Instant.parse("2026-10-22T12:00:00Z"));
            AppSecurityProperties properties = new AppSecurityProperties();
            properties.getLogin().setMaxFailedAttempts(5);
            properties.getLogin().setLockoutMinutes(15);
            properties.getLogin().setIpMaxFailedAttempts(20);
            properties.getLogin().setIpLockoutMinutes(15);
            properties.getLogin().setWindowMinutes(15);
            service = new LoginAttemptService(properties, clock);
        }

        @Test
        void locksByEmailAndIpThenExpiresAndIgnoresStaleFailures() {
            for (int i = 0; i < 4; i++) {
                service.recordFailure(EMAIL, IP);
                assertDoesNotThrow(() -> service.assertNotLocked(EMAIL, IP));
            }

            service.recordFailure(EMAIL, IP);
            ApiBusinessException locked = assertThrows(
                    ApiBusinessException.class,
                    () -> service.assertNotLocked(EMAIL, IP)
            );
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, locked.getStatus());
            assertEquals(ErrorCodes.AUTH_LOGIN_LOCKED, locked.getCode());
            assertEquals(15 * 60, locked.getRetryAfterSeconds());

            clock.advance(Duration.ofMinutes(15));
            assertDoesNotThrow(() -> service.assertNotLocked(EMAIL, IP));

            for (int i = 0; i < 4; i++) {
                service.recordFailure(EMAIL, IP);
            }
            clock.advance(Duration.ofMinutes(16));
            service.recordFailure(EMAIL, IP);
            assertDoesNotThrow(() -> service.assertNotLocked(EMAIL, IP));
        }

        @Test
        void successClearsEmailFailuresAndUnknownEmailsLockTheSameWay() {
            for (int i = 0; i < 4; i++) {
                service.recordFailure(EMAIL, IP);
            }
            service.recordSuccess(EMAIL);
            for (int i = 0; i < 4; i++) {
                service.recordFailure(EMAIL, IP);
            }
            assertDoesNotThrow(() -> service.assertNotLocked(EMAIL, IP));

            for (int i = 0; i < 5; i++) {
                service.recordFailure("Ash@Example.com", IP);
            }
            assertThrows(ApiBusinessException.class, () -> service.assertNotLocked("ash@example.com", IP));

            for (int i = 0; i < 5; i++) {
                service.recordFailure("ghost@example.com", IP);
            }
            assertEquals(
                    ErrorCodes.AUTH_LOGIN_LOCKED,
                    assertThrows(
                            ApiBusinessException.class,
                            () -> service.assertNotLocked("ghost@example.com", "198.51.100.1")
                    ).getCode()
            );

            LoginAttemptService ipService = new LoginAttemptService(new AppSecurityProperties(), clock);
            for (int i = 0; i < 20; i++) {
                ipService.recordFailure("user" + i + "@example.com", IP);
            }
            ApiBusinessException ipLock = assertThrows(
                    ApiBusinessException.class,
                    () -> ipService.assertNotLocked("another@example.com", IP)
            );
            assertEquals(ErrorCodes.AUTH_LOGIN_LOCKED, ipLock.getCode());
            assertNotNull(ipLock.getRetryAfterSeconds());
        }
    }

    @Nested
    class IpRateLimit {

        @Test
        void blocksTheSameBucketAndIpAfterTheMaxHitsAndIsolatesOtherKeys() {
            AuthIpRateLimiter limiter = new AuthIpRateLimiter();
            limiter.consume("register", "203.0.113.10", 2, 15);
            limiter.consume("register", "203.0.113.10", 2, 15);
            ApiBusinessException ex = assertThrows(
                    ApiBusinessException.class,
                    () -> limiter.consume("register", "203.0.113.10", 2, 15)
            );
            assertEquals(ErrorCodes.AUTH_RATE_LIMITED, ex.getCode());

            limiter.consume("reset", "203.0.113.10", 1, 15);
            limiter.consume("register", "198.51.100.1", 1, 15);
        }
    }

    @Nested
    class ClientIp {

        @Test
        void trustsProxyHeadersOnlyBehindLocalAddressesAndNormalizesLoopback() {
            MockHttpServletRequest publicClient = new MockHttpServletRequest();
            publicClient.setRemoteAddr("203.0.113.10");
            publicClient.addHeader("X-Forwarded-For", "198.51.100.1");
            assertEquals("203.0.113.10", ClientIpResolver.resolve(publicClient));

            MockHttpServletRequest localProxy = new MockHttpServletRequest();
            localProxy.setRemoteAddr("127.0.0.1");
            localProxy.addHeader("X-Forwarded-For", "198.51.100.20, 10.0.0.1");
            assertEquals("198.51.100.20", ClientIpResolver.resolve(localProxy));

            MockHttpServletRequest realIp = new MockHttpServletRequest();
            realIp.setRemoteAddr("10.0.0.5");
            realIp.addHeader("X-Real-IP", "198.51.100.30");
            assertEquals("198.51.100.30", ClientIpResolver.resolve(realIp));

            MockHttpServletRequest fallback = new MockHttpServletRequest();
            fallback.setRemoteAddr("192.0.2.8");
            assertEquals("192.0.2.8", ClientIpResolver.resolve(fallback));

            assertTrue(ClientIpResolver.sameClient("::1", "127.0.0.1"));
            assertEquals("127.0.0.1", ClientIpResolver.normalize("::1"));
            assertEquals("127.0.0.1", ClientIpResolver.normalize("[::1]"));
            assertEquals("203.0.113.9", ClientIpResolver.normalize("::ffff:203.0.113.9"));
            assertTrue(ClientIpResolver.sameClient("::ffff:203.0.113.9", "203.0.113.9"));
            assertFalse(ClientIpResolver.sameClient("203.0.113.1", "198.51.100.1"));
        }
    }

    @Nested
    class HttpsRedirect {

        private AppSecurityProperties properties;
        private HttpsEnforcementFilter filter;

        @BeforeEach
        void setUp() {
            properties = new AppSecurityProperties();
            properties.setRequireHttps(true);
            filter = new HttpsEnforcementFilter(properties);
        }

        @Test
        void redirectsPublicHttpToHttpsPreservingThePathAndQuery() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
            request.setScheme("http");
            request.setSecure(false);
            request.setServerName("poketeamguess-backend.onrender.com");
            request.setServerPort(80);
            request.addHeader("X-Forwarded-Proto", "http");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(308, response.getStatus());
            assertEquals("https://poketeamguess-backend.onrender.com/auth/login", response.getHeader("Location"));

            MockHttpServletRequest withQuery = new MockHttpServletRequest("GET", "/api/meta");
            withQuery.setSecure(false);
            withQuery.setServerName("example.com");
            withQuery.setServerPort(443);
            withQuery.setQueryString("page=1");
            withQuery.addHeader("X-Forwarded-Proto", "http");
            MockHttpServletResponse queryResponse = new MockHttpServletResponse();
            filter.doFilter(withQuery, queryResponse, new MockFilterChain());
            assertEquals("https://example.com/api/meta?page=1", queryResponse.getHeader("Location"));
        }

        @Test
        void usesTheConfiguredHostAndRejectsUnsafeHosts() throws Exception {
            properties.setHttpsRedirectHost("poketeamguess-backend.onrender.com");
            MockHttpServletRequest spoofed = new MockHttpServletRequest("GET", "/auth/login");
            spoofed.setSecure(false);
            spoofed.setServerName("evil.example");
            spoofed.addHeader("X-Forwarded-Proto", "http");
            MockHttpServletResponse spoofedResponse = new MockHttpServletResponse();
            filter.doFilter(spoofed, spoofedResponse, new MockFilterChain());
            assertEquals(308, spoofedResponse.getStatus());
            assertEquals(
                    "https://poketeamguess-backend.onrender.com/auth/login",
                    spoofedResponse.getHeader("Location")
            );

            properties.setHttpsRedirectHost("");
            MockHttpServletRequest unsafe = new MockHttpServletRequest("GET", "/auth/login");
            unsafe.setSecure(false);
            unsafe.setServerName("evil.example/@phish");
            unsafe.addHeader("X-Forwarded-Proto", "http");
            MockHttpServletResponse unsafeResponse = new MockHttpServletResponse();
            filter.doFilter(unsafe, unsafeResponse, new MockFilterChain());
            assertEquals(400, unsafeResponse.getStatus());
            assertNull(unsafeResponse.getHeader("Location"));
        }

        @Test
        void skipsHttpsAlreadyPresentHealthChecksAndLocalDevelopment() throws Exception {
            MockHttpServletRequest https = new MockHttpServletRequest("GET", "/api/me");
            https.setSecure(true);
            https.addHeader("X-Forwarded-Proto", "https");
            MockHttpServletResponse httpsResponse = new MockHttpServletResponse();
            filter.doFilter(https, httpsResponse, new MockFilterChain());
            assertEquals(200, httpsResponse.getStatus());
            assertNull(httpsResponse.getHeader("Location"));

            MockHttpServletRequest health = new MockHttpServletRequest("GET", "/api/meta");
            health.setSecure(false);
            MockHttpServletResponse healthResponse = new MockHttpServletResponse();
            filter.doFilter(health, healthResponse, new MockFilterChain());
            assertEquals(200, healthResponse.getStatus());
            assertFalse(HttpsEnforcementFilter.shouldRedirectToHttps(health));

            properties.setRequireHttps(false);
            MockHttpServletRequest local = new MockHttpServletRequest("GET", "/auth/login");
            local.setSecure(false);
            local.addHeader("X-Forwarded-Proto", "http");
            MockHttpServletResponse localResponse = new MockHttpServletResponse();
            filter.doFilter(local, localResponse, new MockFilterChain());
            assertEquals(200, localResponse.getStatus());

            MockHttpServletRequest chain = new MockHttpServletRequest();
            chain.setSecure(false);
            chain.addHeader("X-Forwarded-Proto", "http, https");
            assertTrue(HttpsEnforcementFilter.shouldRedirectToHttps(chain));
        }
    }

    @Nested
    class SessionIpBinding {

        private SessionIpBindingFilter filter;
        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        @BeforeEach
        void setUp() {
            StaticMessageSource messages = new StaticMessageSource();
            messages.addMessage(
                    MessageKeys.SESSION_IP_MISMATCH,
                    Locale.getDefault(),
                    "Sua sessão foi encerrada por motivos de segurança."
            );
            messages.addMessage(
                    MessageKeys.SESSION_BINDING_MISSING,
                    Locale.getDefault(),
                    "Sessão sem IP vinculado."
            );
            filter = new SessionIpBindingFilter(objectMapper, messages);
            LocaleContextHolder.setLocale(Locale.getDefault());
            SecurityContextHolder.clearContext();
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
            LocaleContextHolder.resetLocaleContext();
        }

        @Test
        void keepsAMatchingSessionAndInvalidatesWhenTheIpDivergesOrIsSpoofed() throws Exception {
            MockHttpServletRequest matching = authenticatedRequest("203.0.113.10", "203.0.113.10");
            MockHttpServletResponse matchingResponse = new MockHttpServletResponse();
            filter.doFilter(matching, matchingResponse, new MockFilterChain());
            assertEquals(200, matchingResponse.getStatus());
            assertNotNull(matching.getSession(false));
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());

            MockHttpServletRequest mismatch = authenticatedRequest("203.0.113.10", "198.51.100.1");
            MockHttpServletResponse mismatchResponse = new MockHttpServletResponse();
            filter.doFilter(mismatch, mismatchResponse, new MockFilterChain());
            assertEquals(401, mismatchResponse.getStatus());
            JsonNode body = objectMapper.readTree(mismatchResponse.getContentAsByteArray());
            assertEquals(ErrorCodes.SESSION_IP_MISMATCH, body.get("code").asText());
            assertTrue(body.get("message").asText().toLowerCase().contains("segurança"));
            assertNull(mismatch.getSession(false));
            assertNull(SecurityContextHolder.getContext().getAuthentication());

            MockHttpServletRequest spoofed = authenticatedRequest("203.0.113.10", "198.51.100.1");
            spoofed.addHeader("X-Forwarded-For", "203.0.113.10");
            MockHttpServletResponse spoofedResponse = new MockHttpServletResponse();
            filter.doFilter(spoofed, spoofedResponse, new MockFilterChain());
            assertEquals(401, spoofedResponse.getStatus());
            assertEquals(
                    ErrorCodes.SESSION_IP_MISMATCH,
                    objectMapper.readTree(spoofedResponse.getContentAsByteArray()).get("code").asText()
            );

            MockHttpServletRequest login = new MockHttpServletRequest();
            login.setMethod("POST");
            login.setServletPath("/auth/login");
            login.setRemoteAddr("198.51.100.1");
            MockHttpServletResponse loginResponse = new MockHttpServletResponse();
            filter.doFilter(login, loginResponse, new MockFilterChain());
            assertEquals(200, loginResponse.getStatus());
            assertEquals(0, loginResponse.getContentAsByteArray().length);
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

    @Nested
    class ErrorResponses {

        @AfterEach
        void tearDown() {
            LocaleContextHolder.resetLocaleContext();
        }

        @Test
        void loginLockAddsRetryAfterAndIllegalStateDoesNotLeakInternalMessages() {
            StaticMessageSource messages = new StaticMessageSource();
            messages.addMessage(MessageKeys.AUTH_LOGIN_LOCKED, Locale.ROOT, "Aguarde {0} minuto(s).");
            messages.addMessage(MessageKeys.AUTH_INVALID_CREDENTIALS, Locale.ROOT, "Credenciais inválidas.");
            messages.addMessage(MessageKeys.GAME_MATCH_INVALID_ACTION, Locale.ROOT, "Ação inválida nesta partida.");
            GlobalExceptionHandler handler = new GlobalExceptionHandler(messages);
            LocaleContextHolder.setLocale(Locale.ROOT);

            ApiBusinessException locked = ApiBusinessException.withRetryAfter(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCodes.AUTH_LOGIN_LOCKED,
                    MessageKeys.AUTH_LOGIN_LOCKED,
                    900,
                    15
            );
            ResponseEntity<ApiErrorResponse> lockedResponse = handler.handleBusiness(locked);
            assertEquals(429, lockedResponse.getStatusCode().value());
            assertEquals("900", lockedResponse.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
            assertEquals(ErrorCodes.AUTH_LOGIN_LOCKED, lockedResponse.getBody().code());
            assertEquals("Aguarde 15 minuto(s).", lockedResponse.getBody().message());

            ResponseEntity<ApiErrorResponse> ordinary = handler.handleBusiness(
                    new ApiBusinessException(
                            HttpStatus.UNAUTHORIZED,
                            ErrorCodes.AUTH_INVALID_CREDENTIALS,
                            MessageKeys.AUTH_INVALID_CREDENTIALS
                    )
            );
            assertNull(ordinary.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
            assertEquals(401, ordinary.getStatusCode().value());

            ResponseEntity<ApiErrorResponse> leaked = handler.handleIllegalState(
                    new IllegalStateException("secret stack detail")
            );
            assertEquals(500, leaked.getStatusCode().value());
            assertEquals(ErrorCodes.GAME_MATCH_INVALID_ACTION, leaked.getBody().code());
            assertEquals("Ação inválida nesta partida.", leaked.getBody().message());
        }
    }

    @Nested
    class AuthCodes {

        @Test
        void hasherMatchesTheSameUserPurposeAndCodeAndRejectsTheRest() {
            AppAuthProperties props = new AppAuthProperties();
            props.setCodeSecret("test-secret");
            AuthCodeHasher hasher = new AuthCodeHasher(props);

            String hash = hasher.hash("user-1", AuthCodePurpose.EMAIL_VERIFICATION, "123456");
            assertTrue(hasher.matches("user-1", AuthCodePurpose.EMAIL_VERIFICATION, "123456", hash));
            assertFalse(hasher.matches("user-1", AuthCodePurpose.EMAIL_VERIFICATION, "654321", hash));
            assertFalse(hasher.matches("user-2", AuthCodePurpose.EMAIL_VERIFICATION, "123456", hash));
            assertFalse(hasher.matches("user-1", AuthCodePurpose.EMAIL_VERIFICATION, "123456", null));
        }
    }

    private static final class AdjustableClock extends Clock {
        private Instant instant;

        private AdjustableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
