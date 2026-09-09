package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.admin.AdminUserListItemDto;
import com.svc.pokeguessteam.dto.admin.AdminUserPageResponse;
import com.svc.pokeguessteam.dto.admin.BanUserRequest;
import com.svc.pokeguessteam.dto.admin.SetUserRoleRequest;
import com.svc.pokeguessteam.dto.admin.UnbanUserRequest;
import com.svc.pokeguessteam.service.AdminUserService;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;

    public AdminUserController(AdminUserService adminUserService, CurrentUserService currentUserService) {
        this.adminUserService = adminUserService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<AdminUserPageResponse> listUsers(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean sortByAbandoned,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "ALL") String filter
    ) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(adminUserService.listUsers(adminId, page, size, sortByAbandoned, q, filter));
    }

    @PostMapping("/{userId}/ban")
    public ResponseEntity<AdminUserListItemDto> ban(
            HttpSession session,
            @PathVariable String userId,
            @Valid @RequestBody BanUserRequest request
    ) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(adminUserService.ban(adminId, userId, request));
    }

    @PostMapping("/{userId}/unban")
    public ResponseEntity<AdminUserListItemDto> unban(
            HttpSession session,
            @PathVariable String userId,
            @Valid @RequestBody UnbanUserRequest request
    ) {
        String adminId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(adminUserService.unban(adminId, userId, request.scope()));
    }

    @PostMapping("/{userId}/role")
    public ResponseEntity<AdminUserListItemDto> setRole(
            HttpSession session,
            @PathVariable String userId,
            @Valid @RequestBody SetUserRoleRequest request
    ) {
        String masterId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(adminUserService.setRole(masterId, userId, request));
    }
}
