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
    public UserModel confirmEmailVerification(String email, String plainCode) {
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
        return userRepository.save(user);
    }

    @Transactional
    public void sendEmailChangeCode(UserModel user, String newEmail) {
        String normalizedNewEmail = normalizeEmail(newEmail);
        if (normalizedNewEmail.equals(normalizeEmail(user.getEmail()))) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.AUTH_EMAIL_SAME,
                    MessageKeys.AUTH_EMAIL_SAME
            );
        }
        if (userRepository.findByEmail(normalizedNewEmail).isPresent()) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.AUTH_EMAIL_ALREADY_REGISTERED,
                    MessageKeys.AUTH_EMAIL_ALREADY_REGISTERED
            );
        }
        issueCode(user, AuthCodePurpose.EMAIL_CHANGE, normalizedNewEmail, normalizedNewEmail);
    }

    @Transactional
    public String confirmEmailChangeCode(UserModel user, String newEmail, String plainCode) {
        String normalizedNewEmail = normalizeEmail(newEmail);
        consumeValidCode(user, AuthCodePurpose.EMAIL_CHANGE, plainCode, normalizedNewEmail);
        if (userRepository.findByEmail(normalizedNewEmail)
                .filter(existing -> !existing.getIdUser().equals(user.getIdUser()))
                .isPresent()) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.AUTH_EMAIL_ALREADY_REGISTERED,
                    MessageKeys.AUTH_EMAIL_ALREADY_REGISTERED
            );
        }
        user.setEmail(normalizedNewEmail);
        user.setEmailVerifyTrue();
        userRepository.save(user);
        return normalizedNewEmail;
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
        issueCode(user, purpose, null, user.getEmail());
    }

    private void issueCode(UserModel user, AuthCodePurpose purpose, String targetEmail, String deliverTo) {
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
        row.setTargetEmail(targetEmail);
        row.setCodeHash(authCodeHasher.hash(user.getIdUser(), purpose, plainCode, targetEmail));
        row.setExpiresAt(now.plusMinutes(authProperties.getVerificationCodeExpiryMinutes()));
        authCodeRepository.save(row);

        emailService.sendAuthCode(
                deliverTo,
                purpose,
                plainCode,
                authProperties.getVerificationCodeExpiryMinutes()
        );
    }

    private void consumeValidCode(UserModel user, AuthCodePurpose purpose, String plainCode) {
        consumeValidCode(user, purpose, plainCode, null);
    }

    private void consumeValidCode(UserModel user, AuthCodePurpose purpose, String plainCode, String targetEmail) {
        LocalDateTime now = LocalDateTime.now();
        AuthCodeModel active = authCodeRepository
                .findFirstByUser_IdUserAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getIdUser(), purpose)
                .orElseThrow(AuthCodeService::invalidOrExpiredCode);

        if (!active.isActive(now)) {
            throw invalidOrExpiredCode();
        }

        if (purpose == AuthCodePurpose.EMAIL_CHANGE) {
            String expectedTarget = normalizeEmail(targetEmail);
            String storedTarget = active.getTargetEmail() == null ? "" : normalizeEmail(active.getTargetEmail());
            if (!expectedTarget.equals(storedTarget)) {
                throw invalidOrExpiredCode();
            }
        }

        if (active.getFailedAttempts() >= authProperties.getMaxCodeVerificationAttempts()) {
            active.setConsumedAt(now);
            authCodeRepository.save(active);
            throw invalidOrExpiredCode();
        }

        String hashTarget = purpose == AuthCodePurpose.EMAIL_CHANGE ? active.getTargetEmail() : null;
        if (!authCodeHasher.matches(user.getIdUser(), purpose, plainCode, hashTarget, active.getCodeHash())) {
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
