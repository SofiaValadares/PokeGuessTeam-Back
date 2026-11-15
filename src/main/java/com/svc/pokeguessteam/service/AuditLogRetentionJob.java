package com.svc.pokeguessteam.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRetentionJob {

    private final AuditLogService auditLogService;

    public AuditLogRetentionJob(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /** Purge diário ~03:40 — mantém no máximo {@link AuditLogService#RETENTION_DAYS} dias. */
    @Scheduled(cron = "0 40 3 * * *")
    public void purge() {
        auditLogService.purgeOlderThanRetention();
    }
}
