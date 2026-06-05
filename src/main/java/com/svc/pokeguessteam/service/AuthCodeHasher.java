package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.config.AppAuthProperties;
import com.svc.pokeguessteam.model.auth.AuthCodePurpose;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class AuthCodeHasher {

    private final AppAuthProperties authProperties;

    public AuthCodeHasher(AppAuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String hash(String userId, AuthCodePurpose purpose, String plainCode) {
        return hash(userId, purpose, plainCode, null);
    }

    public String hash(String userId, AuthCodePurpose purpose, String plainCode, String targetEmail) {
        String normalizedTarget = targetEmail == null ? "" : targetEmail.trim().toLowerCase();
        String payload = userId + "|" + purpose.name() + "|" + normalizedTarget + "|"
                + plainCode.trim() + "|" + authProperties.getCodeSecret();
        return sha256(payload);
    }

    public boolean matches(String userId, AuthCodePurpose purpose, String plainCode, String expectedHash) {
        return matches(userId, purpose, plainCode, null, expectedHash);
    }

    public boolean matches(
            String userId,
            AuthCodePurpose purpose,
            String plainCode,
            String targetEmail,
            String expectedHash
    ) {
        return hash(userId, purpose, plainCode, targetEmail).equals(expectedHash);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
