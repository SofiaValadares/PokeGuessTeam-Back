package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.config.AppAuthProperties;
import com.svc.pokeguessteam.repository.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final AuthCodeService authCodeService;
    private final AppAuthProperties authProperties;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ProfileService profileService,
            AuthCodeService authCodeService,
            AppAuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileService = profileService;
        this.authCodeService = authCodeService;
        this.authProperties = authProperties;
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
        UserModel saved = userRepository.save(user);
        profileService.ensureProfileWithStarters(saved.getIdUser());
        authCodeService.sendEmailVerificationCode(saved);
        return saved;
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
        if (authProperties.isRequireEmailVerificationForLogin()
                && !Boolean.TRUE.equals(user.getEmailVerify())) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.AUTH_EMAIL_NOT_VERIFIED,
                    MessageKeys.AUTH_EMAIL_NOT_VERIFIED
            );
        }
        return user;
    }

    @Transactional
    public void resetPasswordWithCode(String email, String code, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.VALIDATION_FAILED,
                    MessageKeys.VALIDATION_REGISTER_PASSWORD_REQUIRED
            );
        }
        UserModel user = authCodeService.confirmPasswordResetCode(email, code);
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

    @Transactional
    public void requestEmailChange(String userId, String newEmail, String currentPassword) {
        UserModel user = requireUser(userId);
        requireCurrentPassword(user, currentPassword);
        authCodeService.sendEmailChangeCode(user, newEmail);
    }

    @Transactional
    public UserModel confirmEmailChange(String userId, String newEmail, String code, String currentPassword) {
        UserModel user = requireUser(userId);
        requireCurrentPassword(user, currentPassword);
        authCodeService.confirmEmailChangeCode(user, newEmail, code);
        return userRepository.findById(userId).orElseThrow();
    }

    /**
     * Marca o utilizador como tendo feito login e indica se este é o primeiro login.
     */
    @Transactional
    public boolean recordLogin(UserModel user) {
        boolean firstLogin = !Boolean.TRUE.equals(user.getHasLoggedIn());
        if (firstLogin) {
            user.setHasLoggedIn(true);
            userRepository.save(user);
        }
        return firstLogin;
    }

    @Transactional(readOnly = true)
    public boolean isEmailVerified(String userId) {
        return userRepository.findById(userId)
                .map(user -> Boolean.TRUE.equals(user.getEmailVerify()))
                .orElse(false);
    }

    private UserModel requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_USER_NOT_FOUND,
                        MessageKeys.PROFILE_USER_NOT_FOUND
                ));
    }

    private void requireCurrentPassword(UserModel user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiBusinessException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodes.AUTH_CURRENT_PASSWORD_WRONG,
                    MessageKeys.AUTH_CURRENT_PASSWORD_WRONG
            );
        }
    }
}