package com.svc.pokeguessteam.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class SocketIoServerConfig {

    @Bean(destroyMethod = "stop")
    public SocketIOServer socketIOServer(AppSocketIoProperties props) {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(props.getHost());
        config.setPort(props.getPort());
        config.setOrigin("*");
        config.setAllowCustomRequests(true);
        config.setJsonSupport(new JacksonJsonSupport(new JavaTimeModule()));
        return new SocketIOServer(config);
    }
}
