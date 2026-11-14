package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security Lab (Unidade 2). Em produção pode ficar ligado, mas só contas com
 * perfil Lab ({@code USER_IS_DEVELOPER}) acedem — ver {@code ROLE_DEVELOPER}.
 */
@ConfigurationProperties(prefix = "app.security-lab")
public class AppSecurityLabProperties {

    /**
     * Kill-switch global. {@code false} desliga API/UI do lab mesmo para DEVs.
     * Default {@code true} para permitir lab em deploy a um grupo seleto (perfil Lab).
     */
    private boolean enabled = true;

    /**
     * Aviso de que o lab em deploy é temporário (cadeira / fase de avaliação).
     */
    private boolean temporaryDeployNotice = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTemporaryDeployNotice() {
        return temporaryDeployNotice;
    }

    public void setTemporaryDeployNotice(boolean temporaryDeployNotice) {
        this.temporaryDeployNotice = temporaryDeployNotice;
    }
}
