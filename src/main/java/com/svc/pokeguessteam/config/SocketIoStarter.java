package com.svc.pokeguessteam.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SocketIoStarter {

    private static final Logger log = LoggerFactory.getLogger(SocketIoStarter.class);

    private final SocketIOServer server;

    public SocketIoStarter(SocketIOServer server) {
        this.server = server;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            server.start();
            log.info("Socket.IO server started on port {}", server.getConfiguration().getPort());
        } catch (Exception ex) {
            log.error(
                    "Socket.IO failed to start (port {} may be in use). "
                            + "REST API remains available; friend-match realtime is disabled until restart. "
                            + "Free the port or set SOCKETIO_PORT.",
                    server.getConfiguration().getPort(),
                    ex
            );
        }
    }
}
