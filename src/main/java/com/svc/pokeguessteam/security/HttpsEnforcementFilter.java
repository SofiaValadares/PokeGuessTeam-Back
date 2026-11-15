package com.svc.pokeguessteam.security;

import com.svc.pokeguessteam.config.AppSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Força HTTPS quando a app está atrás de um proxy que termina TLS (Render, etc.).
 * Pedidos internos sem {@code X-Forwarded-Proto} (health check) não são redirecionados.
 */
@Component
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_HOST = Pattern.compile(
            "(?i)^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*$"
    );

    private final AppSecurityProperties securityProperties;

    public HttpsEnforcementFilter(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !securityProperties.isRequireHttps();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (shouldRedirectToHttps(request)) {
            String location = httpsLocation(request, securityProperties.getHttpsRedirectHost());
            if (location == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            response.setStatus(308);
            response.setHeader("Location", location);
            return;
        }
        filterChain.doFilter(request, response);
    }

    static boolean shouldRedirectToHttps(HttpServletRequest request) {
        if (request.isSecure()) {
            return false;
        }
        String forwarded = firstHop(request.getHeader("X-Forwarded-Proto"));
        return "http".equalsIgnoreCase(forwarded);
    }

    static String httpsLocation(HttpServletRequest request, String configuredHost) {
        String host = configuredHost != null && !configuredHost.isBlank()
                ? configuredHost.trim()
                : request.getServerName();
        if (!isSafeHost(host)) {
            return null;
        }
        StringBuilder location = new StringBuilder("https://").append(host);
        location.append(request.getRequestURI());
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            location.append('?').append(query);
        }
        return location.toString();
    }

    static boolean isSafeHost(String host) {
        return host != null && SAFE_HOST.matcher(host).matches();
    }

    private static String firstHop(String header) {
        if (header == null || header.isBlank()) {
            return "";
        }
        int comma = header.indexOf(',');
        String first = comma < 0 ? header : header.substring(0, comma);
        return first.trim();
    }
}
