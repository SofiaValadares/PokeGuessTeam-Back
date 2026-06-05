package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.resend")
public class AppResendProperties {

    /**
     * API key do painel Resend (prefixo {@code re_}).
     * @see <a href="https://resend.com/docs/api-reference/emails/send-email">Send Email API</a>
     */
    private String apiKey = "";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }
}
