package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.admin.ActiveBonusEventDto;
import com.svc.pokeguessteam.service.BonusEventService;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final BonusEventService bonusEventService;
    private final CurrentUserService currentUserService;

    public EventController(BonusEventService bonusEventService, CurrentUserService currentUserService) {
        this.bonusEventService = bonusEventService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/active")
    public ResponseEntity<ActiveBonusEventDto> active(HttpSession session) {
        currentUserService.requireUserId(session);
        return bonusEventService.findActivePublic()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
