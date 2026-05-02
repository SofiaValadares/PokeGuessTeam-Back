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

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        if (newPassword != null && currentPassword != null && newPassword.equals(currentPassword)) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.AUTH_NEW_PASSWORD_SAME,
                    MessageKeys.AUTH_NEW_PASSWORD_SAME
            );
        }
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_USER_NOT_FOUND,
                        MessageKeys.PROFILE_USER_NOT_FOUND
                ));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiBusinessException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodes.AUTH_CURRENT_PASSWORD_WRONG,
                    MessageKeys.AUTH_CURRENT_PASSWORD_WRONG
            );
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.AUTH_NEW_PASSWORD_SAME,
                    MessageKeys.AUTH_NEW_PASSWORD_SAME
            );
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void changeUsername(String userId, String newUsername, String password) {
        String normalizedUsername = normalizeUsername(newUsername);
        if (normalizedUsername.isEmpty()) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.VALIDATION_FAILED,
                    MessageKeys.VALIDATION_REGISTER_USERNAME_REQUIRED
            );
        }
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_USER_NOT_FOUND,
                        MessageKeys.PROFILE_USER_NOT_FOUND
                ));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiBusinessException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodes.AUTH_CURRENT_PASSWORD_WRONG,
                    MessageKeys.AUTH_CURRENT_PASSWORD_WRONG
            );
        }
        if (normalizedUsername.equals(user.getUsername())) {
            return;
        }
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.AUTH_USERNAME_ALREADY_TAKEN,
                    MessageKeys.AUTH_USERNAME_ALREADY_TAKEN
            );
        }
        user.setUsername(normalizedUsername);
        userRepository.save(user);
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