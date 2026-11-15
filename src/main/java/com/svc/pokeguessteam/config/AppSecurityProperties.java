package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /** Quando true, pedidos HTTP públicos (X-Forwarded-Proto=http) são redirecionados para HTTPS. */
    private boolean requireHttps = false;

    /** Max-Age do HSTS, em segundos (1 ano por defeito). */
    private long hstsMaxAgeSeconds = 31_536_000L;

    /**
     * Host canónico para redirects HTTP→HTTPS. Se vazio, usa o Host do pedido
     * apenas quando for um hostname simples (sem {@code /} ou {@code @}).
     */
    private String httpsRedirectHost = "";

    private final Login login = new Login();

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    public long getHstsMaxAgeSeconds() {
        return hstsMaxAgeSeconds;
    }

    public void setHstsMaxAgeSeconds(long hstsMaxAgeSeconds) {
        this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
    }

    public String getHttpsRedirectHost() {
        return httpsRedirectHost;
    }

    public void setHttpsRedirectHost(String httpsRedirectHost) {
        this.httpsRedirectHost = httpsRedirectHost;
    }

    public Login getLogin() {
        return login;
    }

    public static class Login {

        /** Falhas por e-mail dentro da janela antes do lockout. */
        private int maxFailedAttempts = 5;

        /** Duração do bloqueio por e-mail, em minutos. */
        private int lockoutMinutes = 15;

        /** Falhas por IP dentro da janela antes do lockout. */
        private int ipMaxFailedAttempts = 20;

        /** Duração do bloqueio por IP, em minutos. */
        private int ipLockoutMinutes = 15;

        /** Janela de contagem de falhas, em minutos. */
        private int windowMinutes = 15;

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public int getLockoutMinutes() {
            return lockoutMinutes;
        }

        public void setLockoutMinutes(int lockoutMinutes) {
            this.lockoutMinutes = lockoutMinutes;
        }

        public int getIpMaxFailedAttempts() {
            return ipMaxFailedAttempts;
        }

        public void setIpMaxFailedAttempts(int ipMaxFailedAttempts) {
            this.ipMaxFailedAttempts = ipMaxFailedAttempts;
        }

        public int getIpLockoutMinutes() {
            return ipLockoutMinutes;
        }

        public void setIpLockoutMinutes(int ipLockoutMinutes) {
            this.ipLockoutMinutes = ipLockoutMinutes;
        }

        public int getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(int windowMinutes) {
            this.windowMinutes = windowMinutes;
        }
    }
}
