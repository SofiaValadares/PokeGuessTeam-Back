package com.svc.pokeguessteam.security;

import com.svc.pokeguessteam.model.user.UserModel;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Constrói e atualiza authorities Spring a partir do {@link UserModel#getRole()} da BD.
 * Necessário após promoção a ADMIN sem novo login.
 */
@Service
public class SessionAuthorityService {

    public List<SimpleGrantedAuthority> authoritiesFor(UserModel user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.getRole() != null && user.getRole().isAdminOrAbove()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (user.getRole() != null && user.getRole().isMaster()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MASTER_ADMIN"));
        }
        return authorities;
    }

    public void applyToSession(UserModel user, HttpSession session) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        authoritiesFor(user)
                );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        if (session != null) {
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext
            );
        }
    }
}
