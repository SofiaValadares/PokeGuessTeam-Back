package com.svc.pokeguessteam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pusher")
public class AppPusherProperties {

    private boolean enabled = false;
    private String appId = "";
    private String key = "";
    private String secret = "";
    private String cluster = "mt1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public boolean isConfigured() {
        return enabled
                && appId != null && !appId.isBlank()
                && key != null && !key.isBlank()
                && secret != null && !secret.isBlank()
                && cluster != null && !cluster.isBlank();
    }
}
