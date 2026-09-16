package com.svc.pokeguessteam.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AppLoggerTest {

    @Test
    void logsAllLevelsWithoutThrowing() {
        AppLogger logger = AppLogger.create(AppLoggerTest.class);
        assertDoesNotThrow(() -> {
            logger.debug("logsAllLevelsWithoutThrowing", "debug {}", 1);
            logger.info("logsAllLevelsWithoutThrowing", "info {}", 2);
            logger.warn("logsAllLevelsWithoutThrowing", "warn {}", 3);
            logger.error("logsAllLevelsWithoutThrowing", "error {}", 4);
            logger.error("logsAllLevelsWithoutThrowing", "error with cause", new IllegalStateException("boom"), 5);
        });
    }
}
