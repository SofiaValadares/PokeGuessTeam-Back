package com.svc.pokeguessteam.exception;

public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String MALFORMED_JSON = "MALFORMED_JSON";
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_EMAIL_ALREADY_REGISTERED = "AUTH_EMAIL_ALREADY_REGISTERED";
    public static final String AUTH_USERNAME_ALREADY_TAKEN = "AUTH_USERNAME_ALREADY_TAKEN";
    public static final String AUTH_CURRENT_PASSWORD_WRONG = "AUTH_CURRENT_PASSWORD_WRONG";
    public static final String AUTH_NEW_PASSWORD_SAME = "AUTH_NEW_PASSWORD_SAME";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";
    public static final String SESSION_BINDING_MISSING = "SESSION_BINDING_MISSING";
    public static final String SESSION_BINDING_MISMATCH = "SESSION_BINDING_MISMATCH";
    public static final String SESSION_USER_ID_MISSING = "SESSION_USER_ID_MISSING";

    public static final String PROFILE_USER_NOT_FOUND = "PROFILE_USER_NOT_FOUND";
    public static final String PROFILE_NOT_FOUND = "PROFILE_NOT_FOUND";
}
