package com.svc.pokeguessteam.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Arrays;

/**
 * Fachada reutilizável sobre SLF4J/Logback. O formato de saída
 * {@code [data/hora] [nível] [módulo/função]: mensagem} é definido em {@code logback-spring.xml}.
 */
public final class AppLogger {

    static final String MDC_FUNCTION = "function";

    private final Logger logger;

    private AppLogger(Class<?> type) {
        this.logger = LoggerFactory.getLogger(type);
    }

    public static AppLogger create(Class<?> type) {
        return new AppLogger(type);
    }

    public void debug(String function, String message, Object... args) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        withFunction(function, () -> logger.debug(message, args));
    }

    public void info(String function, String message, Object... args) {
        if (!logger.isInfoEnabled()) {
            return;
        }
        withFunction(function, () -> logger.info(message, args));
    }

    public void warn(String function, String message, Object... args) {
        if (!logger.isWarnEnabled()) {
            return;
        }
        withFunction(function, () -> logger.warn(message, args));
    }

    public void error(String function, String message, Object... args) {
        withFunction(function, () -> logger.error(message, args));
    }

    public void error(String function, String message, Throwable cause, Object... args) {
        withFunction(function, () -> logger.error(message, appendCause(args, cause)));
    }

    private void withFunction(String function, Runnable action) {
        boolean hasFunction = function != null && !function.isBlank();
        if (hasFunction) {
            MDC.put(MDC_FUNCTION, function.trim());
        }
        try {
            action.run();
        } finally {
            if (hasFunction) {
                MDC.remove(MDC_FUNCTION);
            }
        }
    }

    private static Object[] appendCause(Object[] args, Throwable cause) {
        if (cause == null) {
            return args == null ? new Object[0] : args;
        }
        if (args == null || args.length == 0) {
            return new Object[] { cause };
        }
        Object[] withCause = Arrays.copyOf(args, args.length + 1);
        withCause[args.length] = cause;
        return withCause;
    }
}
