package com.svc.pokeguessteam.dto.admin;

import com.svc.pokeguessteam.model.audit.AuditLogModel;
import com.svc.pokeguessteam.model.enums.AuditLogCategory;

import java.time.LocalDateTime;

public record AuditLogEntryDto(
        String id,
        AuditLogCategory category,
        String actorUserId,
        String actorUsername,
        String actorEmail,
        String httpMethod,
        String path,
        Integer statusCode,
        Long durationMs,
        String action,
        String detail,
        LocalDateTime createdAt
) {
    public static AuditLogEntryDto from(AuditLogModel model) {
        return new AuditLogEntryDto(
                model.getId(),
                model.getCategory(),
                model.getActorUserId(),
                model.getActorUsername(),
                model.getActorEmail(),
                model.getHttpMethod(),
                model.getPath(),
                model.getStatusCode(),
                model.getDurationMs(),
                model.getAction(),
                model.getDetail(),
                model.getCreatedAt()
        );
    }
}
