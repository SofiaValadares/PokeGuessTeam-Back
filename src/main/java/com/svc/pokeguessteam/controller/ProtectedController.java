package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.dto.user.MeResponse;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProtectedController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public ProtectedController(CurrentUserService currentUserService, UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    /**
     * {@code authenticatedAs}: igual ao login Spring Security (continua a ser o e-mail — compatibilidade).
     * {@code username} / {@code email}: valores persistidos em {@link UserModel}.
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

        return ResponseEntity.ok(MeResponse.from(authentication, user));
    }
}
