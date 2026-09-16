package com.svc.pokeguessteam.dto.admin;

import org.springframework.data.domain.Page;

import java.util.List;

public record AuditLogPageResponse(
        List<AuditLogEntryDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static AuditLogPageResponse from(Page<AuditLogEntryDto> page) {
        return new AuditLogPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
