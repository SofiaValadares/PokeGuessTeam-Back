package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.admin.SystemLogListResponse;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.SystemLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final SystemLogService systemLogService;
    private final CurrentUserService currentUserService;

    public AdminLogController(SystemLogService systemLogService, CurrentUserService currentUserService) {
        this.systemLogService = systemLogService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<SystemLogListResponse> list(
            HttpSession session,
            @RequestParam(defaultValue = "ALL") String level,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "500") int limit
    ) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(systemLogService.readLogs(adminId, level, q, limit));
    }
}
