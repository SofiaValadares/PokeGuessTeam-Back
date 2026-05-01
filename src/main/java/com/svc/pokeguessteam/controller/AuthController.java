package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.LoginRequest;
import com.svc.pokeguessteam.dto.LoginResponse;
import com.svc.pokeguessteam.dto.RegisterRequest;
import com.svc.pokeguessteam.dto.RegisterResponse;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        UserModel user = authService.register(request.username(), request.email(), request.password());
        profileService.ensureProfileWithStarters(user.getIdUser());

        RegisterResponse response = new RegisterResponse(
                user.getIdUser(),
                user.getEmail(),
                user.getUsername()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                               HttpServletRequest httpRequest) {
        UserModel user = authService.authenticate(request.email(), request.password());
        profileService.ensureProfileWithStarters(user.getIdUser());

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(USER_ID_ATTR, user.getIdUser());
        session.setAttribute(SessionBindingInterceptor.DEVICE_ID_ATTR, DeviceFingerprintUtil.generateDeviceId(httpRequest));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return ResponseEntity.ok(new LoginResponse("Login realizado com sucesso."));
    }
}
