package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.auth.AuthSessionResponse;
import com.svc.pokeguessteam.dto.auth.ChangeEmailConfirmRequest;
import com.svc.pokeguessteam.dto.auth.ChangeEmailRequestRequest;
import com.svc.pokeguessteam.dto.auth.ChangePasswordRequest;
import com.svc.pokeguessteam.dto.auth.ChangeUsernameRequest;
import com.svc.pokeguessteam.dto.auth.DeleteAccountRequest;
import com.svc.pokeguessteam.dto.auth.EmailCodeRequest;
import com.svc.pokeguessteam.dto.auth.EmailOnlyRequest;
import com.svc.pokeguessteam.dto.auth.LoginRequest;
import com.svc.pokeguessteam.dto.auth.MessageResponse;
import com.svc.pokeguessteam.dto.auth.PasswordResetConfirmRequest;
import com.svc.pokeguessteam.dto.auth.RegisterRequest;
import com.svc.pokeguessteam.dto.auth.RegisterResponse;
import com.svc.pokeguessteam.dto.auth.SessionResponse;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.security.DeviceFingerprintUtil;
import com.svc.pokeguessteam.security.SessionBindingInterceptor;
import com.svc.pokeguessteam.service.AccountDeletionService;
import com.svc.pokeguessteam.service.AuthCodeService;
import com.svc.pokeguessteam.service.AuthService;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String USER_ID_ATTR = "USER_ID";

    private final AuthService authService;
    private final AuthCodeService authCodeService;
    private final AccountDeletionService accountDeletionService;
    private final ProfileService profileService;
    private final CurrentUserService currentUserService;
    private final MessageSource messageSource;

    public AuthController(
            AuthService authService,
            AuthCodeService authCodeService,
            AccountDeletionService accountDeletionService,
            ProfileService profileService,
            CurrentUserService currentUserService,
            MessageSource messageSource
    ) {
        this.authService = authService;
        this.authCodeService = authCodeService;
        this.accountDeletionService = accountDeletionService;
        this.profileService = profileService;
        this.currentUserService = currentUserService;
        this.messageSource = messageSource;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        UserModel user = authService.register(
                request.username(),
                request.email(),
                request.password()
        );

        profileService.ensureProfileWithStarters(user.getIdUser());

        return ResponseEntity.ok(
                new RegisterResponse(
                        user.getIdUser(),
                        user.getEmail(),
                        user.getUsername(),
                        Boolean.TRUE.equals(user.getEmailVerify())
                )
        );
    }

    @PostMapping({"/email/verification/send", "/verification/resend"})
    public ResponseEntity<MessageResponse> sendEmailVerification(
            @RequestBody @Valid EmailOnlyRequest request
    ) {
        authCodeService.sendEmailVerificationCodeByEmail(request.email());
        return ResponseEntity.ok(new MessageResponse(msg(MessageKeys.AUTH_EMAIL_VERIFICATION_SENT)));
    }

    @PostMapping({"/email/verification/confirm", "/verification/confirm"})
    public ResponseEntity<AuthSessionResponse> confirmEmailVerification(
            @RequestBody @Valid EmailCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        UserModel user = authCodeService.confirmEmailVerification(request.email(), request.code());
        boolean firstLogin = authService.recordLogin(user);
        establishSession(user, httpRequest);
        return ResponseEntity.ok(toSessionResponse(user, msg(MessageKeys.AUTH_EMAIL_VERIFIED), firstLogin));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<MessageResponse> requestPasswordReset(
            @RequestBody @Valid EmailOnlyRequest request
    ) {
        authCodeService.sendPasswordResetCodeByEmailIfEligible(request.email());
        return ResponseEntity.ok(new MessageResponse(msg(MessageKeys.AUTH_PASSWORD_RESET_SENT)));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<MessageResponse> confirmPasswordReset(
            @RequestBody @Valid PasswordResetConfirmRequest request
    ) {
        authService.resetPasswordWithCode(request.email(), request.code(), request.newPassword());
        return ResponseEntity.ok(new MessageResponse(msg(MessageKeys.AUTH_PASSWORD_RESET_DONE)));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        String userId = currentUserService.requireUserId(session);
        authService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/username")
    public ResponseEntity<Void> changeUsername(
            @RequestBody @Valid ChangeUsernameRequest request,
            HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        String userId = currentUserService.requireUserId(session);
        authService.changeUsername(userId, request.newUsername(), request.password());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/change/request")
    public ResponseEntity<MessageResponse> requestEmailChange(
            @RequestBody @Valid ChangeEmailRequestRequest request,
            HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        String userId = currentUserService.requireUserId(session);
        authService.requestEmailChange(userId, request.newEmail(), request.currentPassword());
        return ResponseEntity.ok(new MessageResponse(msg(MessageKeys.AUTH_EMAIL_CHANGE_SENT)));
    }

    @PostMapping("/email/change/confirm")
    public ResponseEntity<AuthSessionResponse> confirmEmailChange(
            @RequestBody @Valid ChangeEmailConfirmRequest request,
            HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        String userId = currentUserService.requireUserId(session);
        UserModel user = authService.confirmEmailChange(
                userId,
                request.newEmail(),
                request.code(),
                request.currentPassword()
        );
        return ResponseEntity.ok(toSessionResponse(user, msg(MessageKeys.AUTH_EMAIL_CHANGED), false));
    }

    @DeleteMapping("/account")
    public ResponseEntity<MessageResponse> deleteAccount(
            @RequestBody @Valid DeleteAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        String userId = currentUserService.requireUserId(session);
        accountDeletionService.deleteAccount(userId, request.password());
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse(msg(MessageKeys.AUTH_ACCOUNT_DELETED)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthSessionResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        UserModel user = authService.authenticate(
                request.login(),
                request.password()
        );

        boolean firstLogin = authService.recordLogin(user);
        establishSession(user, httpRequest);
        return ResponseEntity.ok(toSessionResponse(user, null, firstLogin));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request
    ) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok().build();
    }

    @GetMapping("/session")
    public ResponseEntity<SessionResponse> session(
            HttpServletRequest request
    ) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        HttpSession session = request.getSession(false);

        boolean authenticated =
                authentication != null &&
                        authentication.isAuthenticated() &&
                        !(authentication instanceof AnonymousAuthenticationToken) &&
                        session != null &&
                        session.getAttribute(USER_ID_ATTR) != null;

        if (!authenticated) {
            return ResponseEntity.ok(
                    new SessionResponse(
                            false,
                            Optional.empty(),
                            Optional.empty()
                    )
            );
        }

        String userId = session.getAttribute(USER_ID_ATTR).toString();
        boolean emailVerified = authService.isEmailVerified(userId);

        return ResponseEntity.ok(
                new SessionResponse(
                        true,
                        Optional.of(userId),
                        Optional.of(emailVerified)
                )
        );
    }

    private void establishSession(UserModel user, HttpServletRequest httpRequest) {
        profileService.ensureProfileWithStarters(user.getIdUser());

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(USER_ID_ATTR, user.getIdUser());
        session.setAttribute(
                SessionBindingInterceptor.DEVICE_ID_ATTR,
                DeviceFingerprintUtil.generateDeviceId(httpRequest)
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );
    }

    private AuthSessionResponse toSessionResponse(UserModel user, String message, boolean firstLogin) {
        return new AuthSessionResponse(
                user.getIdUser(),
                user.getEmail(),
                user.getUsername(),
                Boolean.TRUE.equals(user.getEmailVerify()),
                message,
                firstLogin
        );
    }

    private String msg(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, null, key, locale);
    }
}
