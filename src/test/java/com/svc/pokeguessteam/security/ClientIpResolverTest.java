package com.svc.pokeguessteam.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientIpResolverTest {

    @Test
    void usesRemoteAddrWhenClientIsPublic() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.1");

        assertEquals("203.0.113.10", ClientIpResolver.resolve(request));
    }

    @Test
    void usesFirstXForwardedForHopBehindLocalProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.20, 10.0.0.1");

        assertEquals("198.51.100.20", ClientIpResolver.resolve(request));
    }

    @Test
    void usesXRealIpWhenForwardedForIsAbsentBehindProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Real-IP", "198.51.100.30");

        assertEquals("198.51.100.30", ClientIpResolver.resolve(request));
    }

    @Test
    void fallsBackToRemoteAddrWithoutProxyHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.8");

        assertEquals("192.0.2.8", ClientIpResolver.resolve(request));
    }

    @Test
    void treatsIpv6LoopbackAsLocalhost() {
        assertTrue(ClientIpResolver.sameClient("::1", "127.0.0.1"));
        assertEquals("127.0.0.1", ClientIpResolver.normalize("::1"));
        assertEquals("127.0.0.1", ClientIpResolver.normalize("[::1]"));
    }

    @Test
    void unwrapsIpv4MappedIpv6() {
        assertEquals("203.0.113.9", ClientIpResolver.normalize("::ffff:203.0.113.9"));
        assertTrue(ClientIpResolver.sameClient("::ffff:203.0.113.9", "203.0.113.9"));
    }

    @Test
    void differentPublicIpsAreNotTheSameClient() {
        assertFalse(ClientIpResolver.sameClient("203.0.113.1", "198.51.100.1"));
    }
}
