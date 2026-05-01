package com.svc.pokeguessteam.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public String requireUserId(HttpSession session) {
        Object value = session.getAttribute("USER_ID");
        if (!(value instanceof String userId) || userId.isBlank()) {
            throw new RuntimeException("Sessao sem usuario autenticado.");
        }
        return userId;
    }
}
