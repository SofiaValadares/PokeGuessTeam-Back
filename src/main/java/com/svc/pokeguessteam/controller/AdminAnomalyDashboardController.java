package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.securitylab.SecurityLabDtos;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.SecurityLabService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard de anomalias / resumo de eventos de ataque — painel Admin.
 */
@RestController
@RequestMapping("/api/admin/anomaly-dashboard")
public class AdminAnomalyDashboardController {

    private final SecurityLabService securityLabService;
    private final CurrentUserService currentUserService;

    public AdminAnomalyDashboardController(
            SecurityLabService securityLabService,
            CurrentUserService currentUserService
    ) {
        this.securityLabService = securityLabService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<SecurityLabDtos.DashboardResponse> dashboard(HttpSession session) {
        currentUserService.requireUserId(session);
        return ResponseEntity.ok(securityLabService.dashboard());
    }

    @PostMapping("/scan")
    public ResponseEntity<SecurityLabDtos.AnomalyScanResponse> scan(HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(securityLabService.scanAnomalies(userId));
    }
}
