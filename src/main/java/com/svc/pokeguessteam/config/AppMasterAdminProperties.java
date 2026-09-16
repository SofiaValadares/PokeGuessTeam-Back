package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.master-admin")
public class AppMasterAdminProperties {

    private String bootstrapUsername = "";

    public String getBootstrapUsername() {
        return bootstrapUsername;
    }

    public void setBootstrapUsername(String bootstrapUsername) {
        this.bootstrapUsername =
                bootstrapUsername != null ? bootstrapUsername.trim() : "";
    }

    public boolean isBootstrapUsername(String username) {
        if (username == null || username.isBlank() || bootstrapUsername.isBlank()) {
            return false;
        }

        return bootstrapUsername.equalsIgnoreCase(username.trim());
    }
}