package com.svc.pokeguessteam.config;

import com.svc.pokeguessteam.realtime.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketHandshakeConfig implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            var session = servletRequest.getServletRequest().getSession(false);
            if (session != null) {
                Object userId = session.getAttribute(StompAuthChannelInterceptor.SESSION_USER_ID);
                if (userId instanceof String id && !id.isBlank()) {
                    attributes.put(StompAuthChannelInterceptor.SESSION_USER_ID, id);
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }
}
