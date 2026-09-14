package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.admin.AuditLogEntryDto;
import com.svc.pokeguessteam.dto.admin.AuditLogPageResponse;
import com.svc.pokeguessteam.dto.admin.AuditLogUserCountDto;
import com.svc.pokeguessteam.model.audit.AuditLogModel;
import com.svc.pokeguessteam.model.enums.AuditLogCategory;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.audit.AuditLogRepository;
import com.svc.pokeguessteam.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    public static final int RETENTION_DAYS = 90;
    public static final Set<AuditLogCategory> SYSTEM_CATEGORIES =
            EnumSet.of(AuditLogCategory.ADMIN_ACTION, AuditLogCategory.SECURITY_CRITICAL);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AdminAccessService adminAccessService;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            AdminAccessService adminAccessService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.adminAccessService = adminAccessService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            AuditLogCategory category,
            String actorUserId,
            String httpMethod,
            String path,
            Integer statusCode,
            Long durationMs,
            String action,
            String detail
    ) {
        try {
            AuditLogModel entry = new AuditLogModel();
            entry.setCategory(category);
            entry.setHttpMethod(trimTo(httpMethod, 16));
            entry.setPath(trimTo(path, 512));
            entry.setStatusCode(statusCode);
            entry.setDurationMs(durationMs);
            entry.setAction(trimTo(action == null || action.isBlank() ? "UNKNOWN" : action, 64));
            entry.setDetail(trimTo(detail, 2000));

            if (actorUserId != null && !actorUserId.isBlank()) {
                entry.setActorUserId(actorUserId);
                userRepository.findById(actorUserId).ifPresentOrElse(
                        user -> fillActor(entry, user),
                        () -> {
                            entry.setActorUsername(null);
                            entry.setActorEmail(null);
                        }
                );
            }

            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Falha ao gravar audit log action={}: {}", action, ex.getMessage());
        }
    }

    public void recordAdminAction(String adminId, String action, String detail) {
        record(AuditLogCategory.ADMIN_ACTION, adminId, null, null, null, null, action, detail);
    }

    public void recordSecurity(String actorUserId, String action, String detail) {
        record(AuditLogCategory.SECURITY_CRITICAL, actorUserId, null, null, null, null, action, detail);
    }

    public void recordHttpRequest(
            String actorUserId,
            String method,
            String path,
            int status,
            long durationMs
    ) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return;
        }
        String action = status >= 500 ? "HTTP_ERROR" : status >= 400 ? "HTTP_CLIENT_ERROR" : "HTTP_REQUEST";
        String detail = method + " " + path + " -> " + status + " (" + durationMs + " ms)";
        AuditLogCategory category = status >= 500
                ? AuditLogCategory.SECURITY_CRITICAL
                : AuditLogCategory.USER_REQUEST;
        // Pedidos de admin autenticado em /api/admin (exceto leitura de logs) também como ADMIN_ACTION
        // ficam só como USER_REQUEST aqui; ações mutáveis gravam ADMIN_ACTION nos serviços.
        record(category, actorUserId, method, path, status, durationMs, action, detail);
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse listSystemLogs(String adminId, String q, String category, int page, int size) {
        adminAccessService.requireAdmin(adminId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String query = q == null ? "" : q.trim();

        Set<AuditLogCategory> categories = SYSTEM_CATEGORIES;
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category.trim())) {
            try {
                AuditLogCategory parsed = AuditLogCategory.valueOf(category.trim().toUpperCase());
                if (SYSTEM_CATEGORIES.contains(parsed)) {
                    categories = EnumSet.of(parsed);
                }
            } catch (IllegalArgumentException ignored) {
                /* ALL / inválido → system defaults */
            }
        }

        Page<AuditLogModel> result = auditLogRepository.searchSystemLogs(
                categories,
                query,
                PageRequest.of(safePage, safeSize)
        );
        return AuditLogPageResponse.from(result.map(AuditLogEntryDto::from));
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse listUserLogs(String adminId, String userId, String q, int page, int size) {
        adminAccessService.requireAdmin(adminId);
        if (userId == null || userId.isBlank()) {
            return AuditLogPageResponse.from(Page.empty());
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String query = q == null ? "" : q.trim();
        Page<AuditLogModel> result = auditLogRepository.searchByActorUserId(
                userId.trim(),
                query,
                PageRequest.of(safePage, safeSize)
        );
        return AuditLogPageResponse.from(result.map(AuditLogEntryDto::from));
    }

    @Transactional(readOnly = true)
    public List<AuditLogUserCountDto> listUserLogCounts(String adminId, String q) {
        adminAccessService.requireAdmin(adminId);
        String query = q == null ? "" : q.trim().toLowerCase();

        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : auditLogRepository.countGroupedByActorUserId()) {
            if (row[0] == null) {
                continue;
            }
            counts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        List<AuditLogUserCountDto> items = new ArrayList<>();
        for (UserModel user : userRepository.findAll()) {
            if (!matchesUserQuery(user, query)) {
                continue;
            }
            long count = counts.getOrDefault(user.getIdUser(), 0L);
            items.add(new AuditLogUserCountDto(
                    user.getIdUser(),
                    user.getUsername(),
                    user.getEmail(),
                    count
            ));
        }
        items.sort(Comparator
                .comparingLong(AuditLogUserCountDto::logCount).reversed()
                .thenComparing(AuditLogUserCountDto::username, String.CASE_INSENSITIVE_ORDER));
        return items;
    }

    @Transactional
    public int purgeOlderThanRetention() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = auditLogRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Audit log purge: removed {} entries older than {} days", deleted, RETENTION_DAYS);
        }
        return deleted;
    }

    private static void fillActor(AuditLogModel entry, UserModel user) {
        entry.setActorUsername(user.getUsername());
        entry.setActorEmail(user.getEmail());
    }

    private static boolean matchesUserQuery(UserModel user, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String username = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
        String id = user.getIdUser() != null ? user.getIdUser().toLowerCase() : "";
        return username.contains(query) || email.contains(query) || id.contains(query);
    }

    private static String trimTo(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }
}
