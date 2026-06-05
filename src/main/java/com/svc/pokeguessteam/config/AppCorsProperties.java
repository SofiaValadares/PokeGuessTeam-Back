package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public class AppCorsProperties {

    /**
     * Padrões de origem para CORS HTTP e handshake SockJS.
     * Em produção: {@code APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://app.seudominio.com}.
     */
    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.*:*"
    ));

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns != null
                ? allowedOriginPatterns
                : new ArrayList<>();
    }
}
