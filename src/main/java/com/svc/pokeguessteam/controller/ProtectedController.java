package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.dto.user.MeResponse;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.UserRoleService;
import com.svc.pokeguessteam.security.SessionAuthorityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProtectedController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final SessionAuthorityService sessionAuthorityService;

    public ProtectedController(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            UserRoleService userRoleService,
            SessionAuthorityService sessionAuthorityService
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
        this.sessionAuthorityService = sessionAuthorityService;
    }

    /**
     * {@code authenticatedAs}: igual ao login Spring Security (continua a ser o e-mail — compatibilidade).
     * {@code username} / {@code email}: valores persistidos em {@link UserModel}.
     * Atualiza authorities da sessão a partir do role na BD (ex.: promoção a ADMIN).
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication, HttpSession session) {
        String userId = currentUserService.requireUserId(session);
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_USER_NOT_FOUND,
                        MessageKeys.PROFILE_USER_NOT_FOUND
                ));
        user = userRoleService.syncMasterFromEnv(user);
        sessionAuthorityService.applyToSession(user, session);

        return ResponseEntity.ok(MeResponse.from(
                SecurityContextHolder.getContext().getAuthentication(),
                user
        ));
    }
}
