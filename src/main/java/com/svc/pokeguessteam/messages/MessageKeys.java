package com.svc.pokeguessteam.messages;

/**
 * Chaves de {@code messages.properties}. Use apenas estas constantes ao lançar {@link com.svc.pokeguessteam.exception.ApiBusinessException}.
 */
public final class MessageKeys {

    private MessageKeys() {
    }

    public static final String AUTH_INVALID_CREDENTIALS = "error.auth.invalid-credentials";
    public static final String AUTH_EMAIL_ALREADY_REGISTERED = "error.auth.email-already-registered";
    public static final String AUTH_USERNAME_ALREADY_TAKEN = "error.auth.username-already-taken";
    public static final String AUTH_CURRENT_PASSWORD_WRONG = "error.auth.current-password-wrong";
    public static final String AUTH_NEW_PASSWORD_SAME = "error.auth.new-password-same";

    public static final String SECURITY_AUTHENTICATION_REQUIRED = "error.security.authentication-required";
    public static final String SESSION_INVALID_OR_EXPIRED = "error.session.invalid-or-expired";
    public static final String SESSION_NONE_ACTIVE = "error.session.none-active";
    public static final String SESSION_BINDING_MISSING = "error.session.binding-missing";
    public static final String SESSION_BINDING_MISMATCH = "error.session.binding-mismatch";
    public static final String SESSION_USER_ID_MISSING = "error.session.user-id-missing";

    public static final String VALIDATION_SUMMARY = "error.validation.summary";
    public static final String VALIDATION_FIELD_INVALID = "error.validation.field-invalid";
    public static final String VALIDATION_MALFORMED_JSON = "error.validation.malformed-json";
    public static final String VALIDATION_REGISTER_USERNAME_REQUIRED = "error.validation.register.username.required";

    public static final String PROFILE_USER_NOT_FOUND = "error.profile.user-not-found";
    public static final String PROFILE_NOT_FOUND = "error.profile.not-found";
}
