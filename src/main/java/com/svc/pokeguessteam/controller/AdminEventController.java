package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.admin.BonusEventDto;
import com.svc.pokeguessteam.dto.admin.BonusEventUpsertRequest;
import com.svc.pokeguessteam.service.BonusEventService;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final BonusEventService bonusEventService;
    private final CurrentUserService currentUserService;

    public AdminEventController(BonusEventService bonusEventService, CurrentUserService currentUserService) {
        this.bonusEventService = bonusEventService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<BonusEventDto>> list(HttpSession session) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(bonusEventService.listEvents(adminId));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<BonusEventDto> get(HttpSession session, @PathVariable String eventId) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(bonusEventService.getEvent(adminId, eventId));
    }

    @PostMapping
    public ResponseEntity<BonusEventDto> create(
            HttpSession session,
            @Valid @RequestBody BonusEventUpsertRequest request
    ) {
        String masterId = currentUserService.requireUserId(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(bonusEventService.create(masterId, request));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<BonusEventDto> update(
            HttpSession session,
            @PathVariable String eventId,
            @Valid @RequestBody BonusEventUpsertRequest request
    ) {
        String masterId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(bonusEventService.update(masterId, eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(HttpSession session, @PathVariable String eventId) {
        String masterId = currentUserService.requireUserId(session);
        bonusEventService.delete(masterId, eventId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/start")
    public ResponseEntity<BonusEventDto> start(HttpSession session, @PathVariable String eventId) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(bonusEventService.start(adminId, eventId));
    }
}
