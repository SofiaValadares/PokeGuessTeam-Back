package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.admin.AuditLogPageResponse;
import com.svc.pokeguessteam.dto.admin.AuditLogUserCountDto;
import com.svc.pokeguessteam.service.AuditLogService;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public AdminLogController(AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    /** Logs de sistema: apenas ADMIN_ACTION e SECURITY_CRITICAL. */
    @GetMapping
    public ResponseEntity<AuditLogPageResponse> listSystem(
            HttpSession session,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(auditLogService.listSystemLogs(adminId, q, category, page, size));
    }

    /** Histórico de pedidos/logs atrelados a uma conta. */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<AuditLogPageResponse> listByUser(
            HttpSession session,
            @PathVariable String userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(auditLogService.listUserLogs(adminId, userId, q, page, size));
    }

    /** Lista de utilizadores com contagem de logs (para o modal de cópia/seleção). */
    @GetMapping("/user-counts")
    public ResponseEntity<List<AuditLogUserCountDto>> userCounts(
            HttpSession session,
            @RequestParam(required = false) String q
    ) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(auditLogService.listUserLogCounts(adminId, q));
    }
}
