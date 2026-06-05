package com.svc.pokeguessteam.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SocketIoStarter {

    private final SocketIOServer server;

    public SocketIoStarter(SocketIOServer server) {
        this.server = server;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        server.start();
    }
}
