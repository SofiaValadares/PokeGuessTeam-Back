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
    public static final String AUTH_EMAIL_NOT_VERIFIED = "AUTH_EMAIL_NOT_VERIFIED";
    public static final String AUTH_EMAIL_ALREADY_VERIFIED = "AUTH_EMAIL_ALREADY_VERIFIED";
    public static final String AUTH_EMAIL_NOT_FOUND = "AUTH_EMAIL_NOT_FOUND";
    public static final String AUTH_CODE_INVALID = "AUTH_CODE_INVALID";
    public static final String AUTH_CODE_RESEND_COOLDOWN = "AUTH_CODE_RESEND_COOLDOWN";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";
    public static final String SESSION_BINDING_MISSING = "SESSION_BINDING_MISSING";
    public static final String SESSION_BINDING_MISMATCH = "SESSION_BINDING_MISMATCH";
    public static final String SESSION_USER_ID_MISSING = "SESSION_USER_ID_MISSING";

    public static final String PROFILE_USER_NOT_FOUND = "PROFILE_USER_NOT_FOUND";
    public static final String PROFILE_NOT_FOUND = "PROFILE_NOT_FOUND";
    public static final String TRAINING_TEAM_LINE_NOT_IN_INVENTORY = "TRAINING_TEAM_LINE_NOT_IN_INVENTORY";
    public static final String TRAINING_TEAM_LINE_NOT_FOUND = "TRAINING_TEAM_LINE_NOT_FOUND";
    public static final String TRAINING_TEAM_DUPLICATE = "TRAINING_TEAM_DUPLICATE";

    public static final String POKEMON_SPECIES_NOT_FOUND = "POKEMON_SPECIES_NOT_FOUND";
    public static final String POKEBALL_INSUFFICIENT = "POKEBALL_INSUFFICIENT";
    public static final String POKEMON_POOL_EMPTY_FOR_RARITY = "POKEMON_POOL_EMPTY_FOR_RARITY";

    public static final String GAME_OPPONENT_NAME_REQUIRED = "GAME_OPPONENT_NAME_REQUIRED";
    public static final String GAME_OPPONENT_USER_ID_REQUIRED = "GAME_OPPONENT_USER_ID_REQUIRED";
    public static final String GAME_CANNOT_PLAY_SELF = "GAME_CANNOT_PLAY_SELF";
    public static final String GAME_OPPONENT_NOT_FOUND = "GAME_OPPONENT_NOT_FOUND";
    public static final String GAME_RESULT_INCONSISTENT = "GAME_RESULT_INCONSISTENT";
    public static final String GAME_RESULT_REQUIRED = "GAME_RESULT_REQUIRED";
    public static final String GAME_CORRECT_GUESSES_INVALID = "GAME_CORRECT_GUESSES_INVALID";
    public static final String GAME_OPPONENT_NAME_INVALID = "GAME_OPPONENT_NAME_INVALID";
    public static final String GAME_MATCH_ALREADY_IN_PROGRESS = "GAME_MATCH_ALREADY_IN_PROGRESS";
    public static final String GAME_MATCH_NOT_FOUND = "GAME_MATCH_NOT_FOUND";
    public static final String GAME_MATCH_NOT_ACTIVE = "GAME_MATCH_NOT_ACTIVE";
    public static final String GAME_MATCH_WRONG_TURN = "GAME_MATCH_WRONG_TURN";
    public static final String GAME_MATCH_INVALID_PHASE = "GAME_MATCH_INVALID_PHASE";
    public static final String GAME_MATCH_INVALID_ACTION = "GAME_MATCH_INVALID_ACTION";
    public static final String GAME_TEAM_INVALID = "GAME_TEAM_INVALID";
    public static final String GAME_GUESS_ALREADY_USED = "GAME_GUESS_ALREADY_USED";
    public static final String GAME_FRIEND_MATCH_NOT_FOUND = "GAME_FRIEND_MATCH_NOT_FOUND";
    public static final String GAME_FRIEND_ONLINE_BANNED = "GAME_FRIEND_ONLINE_BANNED";
    public static final String GAME_JOIN_CODE_NOT_FOUND = "GAME_JOIN_CODE_NOT_FOUND";
    public static final String GAME_JOIN_CODE_INVALID = "GAME_JOIN_CODE_INVALID";
    public static final String GAME_MATCH_FULL = "GAME_MATCH_FULL";
    public static final String GAME_MATCH_GUEST_REQUIRED = "GAME_MATCH_GUEST_REQUIRED";
    public static final String GAME_LOCAL_MATCH_NOT_FOUND = "GAME_LOCAL_MATCH_NOT_FOUND";
}
