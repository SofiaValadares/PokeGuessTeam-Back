package com.svc.pokeguessteam.security;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class DeviceFingerprintUtil {

    private DeviceFingerprintUtil() {
    }

    public static String generateDeviceId(HttpServletRequest request) {
        String userAgent = defaultString(request.getHeader("User-Agent"));
        String clientIp = ClientIpResolver.resolve(request);
        return sha256(userAgent + "|" + clientIp);
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

    private static String defaultString(String value) {
        return value == null ? "" : value.trim();
    }
}
