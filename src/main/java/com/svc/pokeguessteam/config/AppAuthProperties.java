package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AppAuthProperties {

    /** Minutos até o código expirar. */
    private int verificationCodeExpiryMinutes = 15;

    /** Intervalo mínimo entre reenvios do mesmo tipo de código. */
    private int verificationResendCooldownSeconds = 60;

    /** Bloqueia login enquanto {@code emailVerify} for falso. */
    private boolean requireEmailVerificationForLogin = true;

    /** Tentativas erradas antes de invalidar o código ativo. */
    private int maxCodeVerificationAttempts = 5;

    /** Segredo para hash do código (definir em produção via env). */
    private String codeSecret = "dev-change-me";

    public int getVerificationCodeExpiryMinutes() {
        return verificationCodeExpiryMinutes;
    }

    public void setVerificationCodeExpiryMinutes(int verificationCodeExpiryMinutes) {
        this.verificationCodeExpiryMinutes = verificationCodeExpiryMinutes;
    }

    public int getVerificationResendCooldownSeconds() {
        return verificationResendCooldownSeconds;
    }

    public void setVerificationResendCooldownSeconds(int verificationResendCooldownSeconds) {
        this.verificationResendCooldownSeconds = verificationResendCooldownSeconds;
    }

    public boolean isRequireEmailVerificationForLogin() {
        return requireEmailVerificationForLogin;
    }

    public void setRequireEmailVerificationForLogin(boolean requireEmailVerificationForLogin) {
        this.requireEmailVerificationForLogin = requireEmailVerificationForLogin;
    }

    public int getMaxCodeVerificationAttempts() {
        return maxCodeVerificationAttempts;
    }

    public void setMaxCodeVerificationAttempts(int maxCodeVerificationAttempts) {
        this.maxCodeVerificationAttempts = maxCodeVerificationAttempts;
    }

    public String getCodeSecret() {
        return codeSecret;
    }

    public void setCodeSecret(String codeSecret) {
        this.codeSecret = codeSecret;
    }
}
