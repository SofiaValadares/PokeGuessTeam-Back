package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Locale;

@ConfigurationProperties(prefix = "app.ai")
public class AppAiProperties {

    private String provider = "auto";

    private String baseUrl = "https://api.openai.com/v1";

    private String apiKey = "";

    private String model = "gpt-4o-mini";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    public boolean usesGemini() {
        if (!isConfigured()) {
            return false;
        }

        String normalizedProvider = provider == null ? "auto" : provider.trim().toLowerCase(Locale.ROOT);
        if ("gemini".equals(normalizedProvider)) {
            return true;
        }
        if ("openai".equals(normalizedProvider)) {
            return false;
        }

        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim().toLowerCase(Locale.ROOT);
        String normalizedModel = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        String normalizedApiKey = apiKey.trim();

        return normalizedBaseUrl.contains("googleapis")
                || normalizedModel.startsWith("gemini")
                || normalizedApiKey.startsWith("AIza");
    }

    public String resolveGeminiBaseUrl() {
        if (StringUtils.hasText(baseUrl) && baseUrl.toLowerCase(Locale.ROOT).contains("googleapis")) {
            return baseUrl;
        }
        return "https://generativelanguage.googleapis.com";
    }

    public String resolveGeminiModel() {
        String normalizedModel = model == null ? "" : model.trim();
        if (normalizedModel.startsWith("models/")) {
            normalizedModel = normalizedModel.substring("models/".length());
        }
        return StringUtils.hasText(normalizedModel) ? normalizedModel : "gemini-2.5-flash";
    }
}