package com.svc.pokeguessteam.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Regista o ciclo de vida HTTP das rotas (método, caminho, estado, duração).
 * Não lê o corpo do pedido para não vazar credenciais.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = RequestLoggingInterceptor.class.getName() + ".startNanos";
    private static final AppLogger log = AppLogger.create(RequestLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isSkippable(request)) {
            return true;
        }
        request.setAttribute(START_ATTR, System.nanoTime());
        log.debug("preHandle", "{} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        if (isSkippable(request)) {
            return;
        }
        Object start = request.getAttribute(START_ATTR);
        long elapsedMs = start instanceof Long
                ? (System.nanoTime() - (Long) start) / 1_000_000L
                : -1L;
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (ex != null) {
            log.error(
                    "afterCompletion",
                    "Pedido falhou: {} {} -> {} ({} ms)",
                    ex,
                    method,
                    uri,
                    status,
                    elapsedMs
            );
            return;
        }
        if (status >= 500) {
            log.error("afterCompletion", "{} {} -> {} ({} ms)", method, uri, status, elapsedMs);
        } else if (status >= 400) {
            log.warn("afterCompletion", "{} {} -> {} ({} ms)", method, uri, status, elapsedMs);
        } else {
            log.info("afterCompletion", "{} {} -> {} ({} ms)", method, uri, status, elapsedMs);
        }
    }

    private static boolean isSkippable(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri != null && (uri.equals("/error") || uri.equals("/favicon.ico"));
    }
}
