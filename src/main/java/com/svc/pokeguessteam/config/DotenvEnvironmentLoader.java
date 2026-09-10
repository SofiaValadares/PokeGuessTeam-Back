package com.svc.pokeguessteam.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Carrega variáveis de um ficheiro {@code .env} na raiz do projeto para as system properties,
 * sem sobrescrever variáveis de ambiente já definidas no SO.
 */
public final class DotenvEnvironmentLoader {

    private DotenvEnvironmentLoader() {
    }

    public static void load() {
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .filename(".env")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, entry.getValue());
            }
        });
    }
}
