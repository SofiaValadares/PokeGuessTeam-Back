package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.UserModel;
import com.svc.pokeguessteam.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserModel register(String username, String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedUsername = normalizeUsername(username);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.AUTH_EMAIL_ALREADY_REGISTERED,
                    MessageKeys.AUTH_EMAIL_ALREADY_REGISTERED
            );
        }

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.AUTH_USERNAME_ALREADY_TAKEN,
                    MessageKeys.AUTH_USERNAME_ALREADY_TAKEN
            );
        }

        UserModel user = new UserModel();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    public UserModel authenticate(String login, String rawPassword) {
        String trimmedLogin = login == null ? "" : login.trim();
        if (trimmedLogin.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw invalidCredentials();
        }

        UserModel user = findUserByLogin(trimmedLogin)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return user;
    }

    private ApiBusinessException invalidCredentials() {
        return new ApiBusinessException(
                HttpStatus.UNAUTHORIZED,
                ErrorCodes.AUTH_INVALID_CREDENTIALS,
                MessageKeys.AUTH_INVALID_CREDENTIALS
        );
    }

    private Optional<UserModel> findUserByLogin(String login) {
        if (login.contains("@")) {
            return userRepository.findByEmail(normalizeEmail(login));
        }

        return userRepository.findByUsername(normalizeUsername(login));
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim();
    }
}