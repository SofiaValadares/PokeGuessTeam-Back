package com.svc.pokeguessteam.security;

import com.svc.pokeguessteam.repository.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class DatabaseRoleRefreshFilter extends OncePerRequestFilter {

    private static final String USER_ID_ATTR = "USER_ID";

    private final UserRepository userRepository;
    private final SessionAuthorityService sessionAuthorityService;

    public DatabaseRoleRefreshFilter(
            UserRepository userRepository,
            SessionAuthorityService sessionAuthorityService
    ) {
        this.userRepository = userRepository;
        this.sessionAuthorityService = sessionAuthorityService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            Object userId = session.getAttribute(USER_ID_ATTR);

            if (userId != null) {
                userRepository.findById(userId.toString())
                        .ifPresentOrElse(
                                user -> sessionAuthorityService
                                        .applyToSession(user, session),
                                () -> {
                                    session.invalidate();
                                    SecurityContextHolder.clearContext();
                                }
                        );
            }
        }

        filterChain.doFilter(request, response);
    }
}