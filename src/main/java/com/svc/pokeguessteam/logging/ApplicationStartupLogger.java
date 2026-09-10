package com.svc.pokeguessteam.logging;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Confirma que o Logback já está ativo após o arranque do Spring.
 * A configuração efetiva vive em {@code logback-spring.xml} (carregado automaticamente).
 */
@Component
public class ApplicationStartupLogger {

    private static final AppLogger log = AppLogger.create(ApplicationStartupLogger.class);

    private final Environment environment;

    public ApplicationStartupLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String appName = environment.getProperty("spring.application.name", "pokeguessteam");
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        log.info("onApplicationReady", "Aplicação {} pronta na porta {}", appName, port);
    }
}
