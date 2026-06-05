package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dev-tools")
public class AppDevToolsProperties {

    /** Endpoints de debug (ex.: ajuste de XP). Desligar em produção. */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
