package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public class AppMailProperties {

    private String from = "PokeTeamGuess <noreply@localhost>";

    /**
     * Em dev: se true, não envia e-mail — apenas regista o código no log.
     */
    private boolean devLogOnly = true;

    /**
     * Em dev: se o envio direto falhar (ex.: Resend sandbox), reenvia para este endereço
     * com o destinatário original no corpo. Ignorado quando {@link #devLogOnly} está ativo.
     */
    private String devRedirectTo = "";

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getDevRedirectTo() {
        return devRedirectTo;
    }

    public void setDevRedirectTo(String devRedirectTo) {
        this.devRedirectTo = devRedirectTo;
    }

    public boolean isDevLogOnly() {
        return devLogOnly;
    }

    public void setDevLogOnly(boolean devLogOnly) {
        this.devLogOnly = devLogOnly;
    }

    public boolean hasDevRedirectTo() {
        return devRedirectTo != null && !devRedirectTo.isBlank();
    }
}
