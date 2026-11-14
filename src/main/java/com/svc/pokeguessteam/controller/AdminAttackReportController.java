package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.model.enums.SecurityLabEventType;
import com.svc.pokeguessteam.model.securitylab.SecurityLabEventModel;
import com.svc.pokeguessteam.repository.securitylab.SecurityLabEventRepository;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/attack-reports")
public class AdminAttackReportController {

    private final SecurityLabEventRepository eventRepository;
    private final CurrentUserService currentUserService;

    public AdminAttackReportController(
            SecurityLabEventRepository eventRepository,
            CurrentUserService currentUserService
    ) {
        this.eventRepository = eventRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean developerOnly,
            @RequestParam(required = false) String q
    ) {
        currentUserService.requireUserId(session);
        SecurityLabEventType eventType = null;
        if (type != null && !type.isBlank()) {
            eventType = SecurityLabEventType.valueOf(type.trim().toUpperCase());
        }
        Page<SecurityLabEventModel> result = eventRepository.searchAttacks(
                eventType,
                developerOnly,
                q == null ? "" : q.trim(),
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", result.getContent().stream().map(this::toDto).toList());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        body.put("first", result.isFirst());
        body.put("last", result.isLast());
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toDto(SecurityLabEventModel e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("eventType", e.getEventType().name());
        m.put("actorUserId", e.getActorUserId());
        m.put("actorUsername", e.getActorUsername());
        m.put("actorWasDeveloper", e.isActorWasDeveloper());
        m.put("summary", e.getSummary());
        m.put("detailJson", e.getDetailJson());
        m.put("metricValue", e.getMetricValue());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }
}
