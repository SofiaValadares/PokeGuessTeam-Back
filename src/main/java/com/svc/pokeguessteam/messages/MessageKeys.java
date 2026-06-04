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
    public static final String TRAINING_TEAM_LINE_NOT_IN_INVENTORY = "error.profile.training-team.not-in-inventory";
    public static final String TRAINING_TEAM_LINE_NOT_FOUND = "error.profile.training-team.line-not-found";
    public static final String TRAINING_TEAM_DUPLICATE = "error.profile.training-team.duplicate";

    public static final String POKEMON_SPECIES_NOT_FOUND = "error.pokemon.species-not-found";
    public static final String POKEBALL_INSUFFICIENT = "error.pokemon.pokeball-insufficient";
    public static final String POKEMON_POOL_EMPTY_FOR_RARITY = "error.pokemon.pool-empty-for-rarity";

    public static final String GAME_OPPONENT_NAME_REQUIRED = "error.game.opponent-name.required";
    public static final String GAME_OPPONENT_USER_ID_REQUIRED = "error.game.opponent-user-id.required";
    public static final String GAME_CANNOT_PLAY_SELF = "error.game.cannot-play-self";
    public static final String GAME_OPPONENT_NOT_FOUND = "error.game.opponent-not-found";
    public static final String GAME_CORRECT_GUESSES_MIN = "error.game.correct-guesses.min";
    public static final String GAME_CORRECT_GUESSES_MAX = "error.game.correct-guesses.max";
    public static final String GAME_RESULT_REQUIRED = "error.game.result.required";
    public static final String GAME_RESULT_INCONSISTENT = "error.game.result-inconsistent";
    public static final String GAME_OPPONENT_NAME_SIZE = "error.game.opponent-name.size";
    public static final String GAME_MATCH_ALREADY_IN_PROGRESS = "error.game.match-already-in-progress";
    public static final String GAME_MATCH_NOT_FOUND = "error.game.match-not-found";
    public static final String GAME_MATCH_NOT_ACTIVE = "error.game.match-not-active";
    public static final String GAME_MATCH_WRONG_TURN = "error.game.match-wrong-turn";
    public static final String GAME_MATCH_INVALID_PHASE = "error.game.match-invalid-phase";
    public static final String GAME_MATCH_INVALID_ACTION = "error.game.match-invalid-action";
    public static final String GAME_TEAM_INVALID = "error.game.team-invalid";
    public static final String GAME_TEAM_DUPLICATE = "error.game.team-duplicate";
    public static final String GAME_GUESS_ALREADY_USED = "error.game.guess-already-used";
    public static final String GAME_FRIEND_MATCH_NOT_FOUND = "error.game.friend-match-not-found";
    public static final String GAME_FRIEND_ONLINE_BANNED = "error.game.friend-online-banned";
    public static final String GAME_JOIN_CODE_NOT_FOUND = "error.game.join-code-not-found";
    public static final String GAME_JOIN_CODE_INVALID = "error.game.join-code.invalid";
    public static final String GAME_JOIN_CODE_REQUIRED = "error.game.join-code.required";
    public static final String GAME_JOIN_CODE_SIZE = "error.game.join-code.size";
    public static final String GAME_MATCH_FULL = "error.game.match-full";
    public static final String GAME_MATCH_GUEST_REQUIRED = "error.game.match-guest-required";
    public static final String GAME_MATCH_TEAM_LOCKED = "error.game.match-team-locked";
    public static final String GAME_LOCAL_MATCH_NOT_FOUND = "error.game.local-match-not-found";
}
