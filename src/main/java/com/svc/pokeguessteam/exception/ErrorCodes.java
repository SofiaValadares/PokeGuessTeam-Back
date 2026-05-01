package com.svc.pokeguessteam.exception;

public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String MALFORMED_JSON = "MALFORMED_JSON";
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_EMAIL_ALREADY_REGISTERED = "AUTH_EMAIL_ALREADY_REGISTERED";
    public static final String AUTH_USERNAME_ALREADY_TAKEN = "AUTH_USERNAME_ALREADY_TAKEN";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";
    public static final String SESSION_BINDING_MISSING = "SESSION_BINDING_MISSING";
    public static final String SESSION_BINDING_MISMATCH = "SESSION_BINDING_MISMATCH";
    public static final String SESSION_USER_ID_MISSING = "SESSION_USER_ID_MISSING";

    public static final String PROFILE_USER_NOT_FOUND = "PROFILE_USER_NOT_FOUND";
    public static final String PROFILE_NOT_FOUND = "PROFILE_NOT_FOUND";

    public static final String GAME_TEAM_SIZE = "GAME_TEAM_SIZE";
    public static final String GAME_POKEDEX_INSUFFICIENT = "GAME_POKEDEX_INSUFFICIENT";
    public static final String GAME_MATCH_FINISHED = "GAME_MATCH_FINISHED";
    public static final String GAME_NOT_YOUR_TURN = "GAME_NOT_YOUR_TURN";
    public static final String GAME_MATCH_NOT_FOUND = "GAME_MATCH_NOT_FOUND";
    public static final String GAME_POKEMON_NOT_FOUND = "GAME_POKEMON_NOT_FOUND";
    public static final String GAME_USER_NOT_IN_MATCH = "GAME_USER_NOT_IN_MATCH";
}
