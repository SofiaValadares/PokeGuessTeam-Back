package com.svc.pokeguessteam.logging;

import com.svc.pokeguessteam.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Regista o ciclo de vida HTTP das rotas (método, caminho, estado, duração).
 * Não lê o corpo do pedido para não vazar credenciais.
 * Grava auditoria estruturada (USER_REQUEST / erros críticos) excluindo health/static/polling.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = RequestLoggingInterceptor.class.getName() + ".startNanos";
    private static final AppLogger log = AppLogger.create(RequestLoggingInterceptor.class);

    private final AuditLogService auditLogService;

    public RequestLoggingInterceptor(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

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
        } else if (status >= 500) {
            log.error("afterCompletion", "{} {} -> {} ({} ms)", method, uri, status, elapsedMs);
        } else if (status >= 400) {
            log.warn("afterCompletion", "{} {} -> {} ({} ms)", method, uri, status, elapsedMs);
        } else {
            log.info("afterCompletion", "{} {} -> {} ({} ms)", method, uri, status, elapsedMs);
        }

        if (shouldSkipAudit(request)) {
            return;
        }
        String userId = resolveUserId(request);
        if (userId == null) {
            if (status == 401 || status == 403) {
                auditLogService.recordSecurity(
                        null,
                        "HTTP_UNAUTHORIZED",
                        method + " " + uri + " -> " + status
                );
            }
            return;
        }
        auditLogService.recordHttpRequest(userId, method, uri, status, elapsedMs);
    }

    private static String resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute("USER_ID");
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    private static boolean isSkippable(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri != null && (uri.equals("/error") || uri.equals("/favicon.ico"));
    }

    /** Exclui health/static e polling frequente da auditoria estruturada. */
    private static boolean shouldSkipAudit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        String path = uri.toLowerCase();
        if (path.startsWith("/actuator")
                || path.startsWith("/static")
                || path.startsWith("/assets")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".map")
                || path.endsWith(".ico")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".webp")
                || path.endsWith(".svg")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if (path.equals("/api/me")
                    || path.equals("/api/health")
                    || path.startsWith("/api/pusher")
                    || path.equals("/api/game/friend")
                    || path.startsWith("/api/game/competitive")
                    || path.startsWith("/api/admin/logs")) {
                return true;
            }
        }
        return false;
    }
}
