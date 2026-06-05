package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.config.AppAuthProperties;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.auth.AuthCodeModel;
import com.svc.pokeguessteam.model.auth.AuthCodePurpose;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.auth.AuthCodeRepository;
import com.svc.pokeguessteam.repository.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthCodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthCodeRepository authCodeRepository;
    private final UserRepository userRepository;
    private final AuthCodeHasher authCodeHasher;
    private final TransactionalEmailService emailService;
    private final AppAuthProperties authProperties;

    public AuthCodeService(
            AuthCodeRepository authCodeRepository,
            UserRepository userRepository,
            AuthCodeHasher authCodeHasher,
            TransactionalEmailService emailService,
            AppAuthProperties authProperties
    ) {
        this.authCodeRepository = authCodeRepository;
        this.userRepository = userRepository;
        this.authCodeHasher = authCodeHasher;
        this.emailService = emailService;
        this.authProperties = authProperties;
    }

    @Transactional
    public void sendEmailVerificationCode(UserModel user) {
        if (Boolean.TRUE.equals(user.getEmailVerify())) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.AUTH_EMAIL_ALREADY_VERIFIED,
                    MessageKeys.AUTH_EMAIL_ALREADY_VERIFIED
            );
        }
        issueCode(user, AuthCodePurpose.EMAIL_VERIFICATION);
    }

    @Transactional
    public void sendEmailVerificationCodeByEmail(String email) {
        UserModel user = requireUserByEmail(email);
        sendEmailVerificationCode(user);
    }

    /**
     * Não revela se o e-mail existe ou está verificado (evita enumeração).
     */
    @Transactional
    public void sendPasswordResetCodeByEmailIfEligible(String email) {
        userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> Boolean.TRUE.equals(user.getEmailVerify()))
                .ifPresent(user -> issueCode(user, AuthCodePurpose.PASSWORD_RESET));
    }

    @Transactional
    public void confirmEmailVerification(String email, String plainCode) {
        UserModel user = requireUserByEmail(email);
        if (Boolean.TRUE.equals(user.getEmailVerify())) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.AUTH_EMAIL_ALREADY_VERIFIED,
                    MessageKeys.AUTH_EMAIL_ALREADY_VERIFIED
            );
        }
        consumeValidCode(user, AuthCodePurpose.EMAIL_VERIFICATION, plainCode);
        user.setEmailVerifyTrue();
        userRepository.save(user);
    }

    @Transactional
    public UserModel confirmPasswordResetCode(String email, String plainCode) {
        UserModel user = requireUserByEmail(email);
        if (!Boolean.TRUE.equals(user.getEmailVerify())) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.AUTH_EMAIL_NOT_VERIFIED,
                    MessageKeys.AUTH_EMAIL_NOT_VERIFIED
            );
        }
        consumeValidCode(user, AuthCodePurpose.PASSWORD_RESET, plainCode);
        return user;
    }

    private void issueCode(UserModel user, AuthCodePurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        authCodeRepository.findFirstByUser_IdUserAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                        user.getIdUser(),
                        purpose
                )
                .ifPresent(active -> {
                    if (active.getCreatedAt().plusSeconds(authProperties.getVerificationResendCooldownSeconds()).isAfter(now)) {
                        throw new ApiBusinessException(
                                HttpStatus.TOO_MANY_REQUESTS,
                                ErrorCodes.AUTH_CODE_RESEND_COOLDOWN,
                                MessageKeys.AUTH_CODE_RESEND_COOLDOWN
                        );
                    }
                });

        authCodeRepository.consumeActiveCodes(user.getIdUser(), purpose, now);

        String plainCode = generateNumericCode();
        AuthCodeModel row = new AuthCodeModel();
        row.setUser(user);
        row.setPurpose(purpose);
        row.setCodeHash(authCodeHasher.hash(user.getIdUser(), purpose, plainCode));
        row.setExpiresAt(now.plusMinutes(authProperties.getVerificationCodeExpiryMinutes()));
        authCodeRepository.save(row);

        emailService.sendAuthCode(
                user.getEmail(),
                purpose,
                plainCode,
                authProperties.getVerificationCodeExpiryMinutes()
        );
    }

    private void consumeValidCode(UserModel user, AuthCodePurpose purpose, String plainCode) {
        LocalDateTime now = LocalDateTime.now();
        AuthCodeModel active = authCodeRepository
                .findFirstByUser_IdUserAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getIdUser(), purpose)
                .orElseThrow(AuthCodeService::invalidOrExpiredCode);

        if (!active.isActive(now)) {
            throw invalidOrExpiredCode();
        }

        if (active.getFailedAttempts() >= authProperties.getMaxCodeVerificationAttempts()) {
            active.setConsumedAt(now);
            authCodeRepository.save(active);
            throw invalidOrExpiredCode();
        }

        if (!authCodeHasher.matches(user.getIdUser(), purpose, plainCode, active.getCodeHash())) {
            active.incrementFailedAttempts();
            authCodeRepository.save(active);
            throw invalidOrExpiredCode();
        }

        active.setConsumedAt(now);
        authCodeRepository.save(active);
    }

    private UserModel requireUserByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.AUTH_EMAIL_NOT_FOUND,
                        MessageKeys.AUTH_EMAIL_NOT_FOUND
                ));
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    private static String generateNumericCode() {
        int value = SECURE_RANDOM.nextInt(100_000_000);
        return String.format("%08d", value);
    }

    private static ApiBusinessException invalidOrExpiredCode() {
        return new ApiBusinessException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.AUTH_CODE_INVALID,
                MessageKeys.AUTH_CODE_INVALID
        );
    }
}
