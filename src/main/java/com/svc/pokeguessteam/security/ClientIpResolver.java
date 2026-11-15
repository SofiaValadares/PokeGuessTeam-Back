package com.svc.pokeguessteam.security;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Resolve o IP do cliente considerando proxies/load balancers.
 * <p>
 * Quando o pedido chega de loopback/rede privada (proxy local ou
 * {@code ForwardedHeaderFilter} ainda não reescreveu o endereço), usa o primeiro
 * salto de {@code X-Forwarded-For} ou {@code X-Real-IP}. Pedidos vindos de um IP
 * público usam {@code getRemoteAddr()} e ignoram cabeçalhos forjados pelo cliente.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String remoteAddr = normalize(request.getRemoteAddr());
        if (isInternalAddress(remoteAddr)) {
            String forwarded = firstForwardedClient(request.getHeader("X-Forwarded-For"));
            if (!forwarded.isEmpty()) {
                return forwarded;
            }
            String realIp = normalize(request.getHeader("X-Real-IP"));
            if (!realIp.isEmpty()) {
                return realIp;
            }
        }
        return remoteAddr;
    }

    public static boolean sameClient(String boundIp, String currentIp) {
        return normalize(boundIp).equals(normalize(currentIp));
    }

    static String normalize(String ip) {
        if (ip == null) {
            return "";
        }
        String value = ip.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (value.startsWith("[") && value.endsWith("]") && value.length() > 2) {
            value = value.substring(1, value.length() - 1);
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("::ffff:")) {
            value = value.substring(7);
            lower = value.toLowerCase(Locale.ROOT);
        }
        if ("::1".equals(lower) || "0:0:0:0:0:0:0:1".equals(lower)) {
            return "127.0.0.1";
        }
        return lower;
    }

    private static String firstForwardedClient(String xForwardedFor) {
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return "";
        }
        for (String hop : xForwardedFor.split(",")) {
            String normalized = normalize(hop);
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return "";
    }

    static boolean isInternalAddress(String ip) {
        String normalized = normalize(ip);
        if (normalized.isEmpty()) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(normalized);
            return addr.isAnyLocalAddress()
                    || addr.isLoopbackAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress();
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
