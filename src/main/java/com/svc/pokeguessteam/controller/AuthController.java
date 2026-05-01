package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.auth.LoginRequest;
import com.svc.pokeguessteam.dto.auth.RegisterRequest;
import com.svc.pokeguessteam.dto.auth.RegisterResponse;
import com.svc.pokeguessteam.dto.auth.SessionResponse;
import com.svc.pokeguessteam.model.UserModel;
import com.svc.pokeguessteam.security.DeviceFingerprintUtil;
import com.svc.pokeguessteam.security.SessionBindingInterceptor;
import com.svc.pokeguessteam.service.AuthService;
import com.svc.pokeguessteam.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String USER_ID_ATTR = "USER_ID";

    private final AuthService authService;
    private final ProfileService profileService;

    public AuthController(AuthService authService, ProfileService profileService) {
        this.authService = authService;
        this.profileService = profileService;
    }

    /**
     * REGISTRO
     */
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
                        user.getUsername()
                )
        );
    }

    /**
     * LOGIN
     */
    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        // Autentica usuário
        UserModel user = authService.authenticate(
                request.email(),
                request.password()
        );

        profileService.ensureProfileWithStarters(user.getIdUser());

        // Cria sessão
        HttpSession session = httpRequest.getSession(true);

        // Salva ID
        session.setAttribute(USER_ID_ATTR, user.getIdUser());

        // Salva fingerprint do dispositivo
        session.setAttribute(
                SessionBindingInterceptor.DEVICE_ID_ATTR,
                DeviceFingerprintUtil.generateDeviceId(httpRequest)
        );

        // Cria autenticação
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        // Cria contexto
        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Persiste contexto
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        return ResponseEntity.ok().build();
    }

    /**
     * LOGOUT
     */
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

    /**
     * VERIFICAR STATUS DE LOGIN
     */
    @GetMapping("/session")
    public ResponseEntity<SessionResponse> session(
            HttpServletRequest request
    ) {

        // Obtém autenticação atual do Spring Security
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // Obtém sessão atual sem criar nova
        HttpSession session = request.getSession(false);

        /**
         * Regras para considerar usuário autenticado:
         *
         * 1. Authentication existe
         * 2. Está autenticado
         * 3. Sessão existe
         * 4. USER_ID existe na sessão
         */
        boolean authenticated =
                authentication != null &&
                        authentication.isAuthenticated() &&
                        session != null &&
                        session.getAttribute(USER_ID_ATTR) != null;

        // Caso não autenticado
        if (!authenticated) {
            return ResponseEntity.ok(
                    new SessionResponse(
                            false,
                            Optional.empty()
                    )
            );
        }

        // Obtém ID do usuário da sessão
        String userId = session.getAttribute(USER_ID_ATTR).toString();

        // Retorna sessão válida
        return ResponseEntity.ok(
                new SessionResponse(
                        true,
                        Optional.of(userId)
                )
        );
    }
}