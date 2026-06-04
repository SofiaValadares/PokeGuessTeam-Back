package com.svc.pokeguessteam.config;

import com.svc.pokeguessteam.realtime.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final WebSocketHandshakeConfig webSocketHandshakeConfig;

    public WebSocketConfig(
            StompAuthChannelInterceptor stompAuthChannelInterceptor,
            WebSocketHandshakeConfig webSocketHandshakeConfig
    ) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.webSocketHandshakeConfig = webSocketHandshakeConfig;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(webSocketHandshakeConfig)
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://localhost:5500",
                        "http://127.0.0.1:5500"
                )
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
