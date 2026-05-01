package com.svc.pokeguessteam.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProtectedController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication, HttpSession session) {
        return ResponseEntity.ok(Map.of(
                "authenticatedAs", authentication.getName(),
                "userId", session.getAttribute("USER_ID")
        ));
    }
}
