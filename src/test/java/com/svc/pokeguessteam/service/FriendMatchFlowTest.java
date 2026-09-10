package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.model.enums.GuessOutcome;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.util.MatchEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo crítico do modo amigo: partida em memória, turnos e palpites.
 */
class FriendMatchFlowTest {

    private static final List<Integer> HOST_TEAM = List.of(1, 2, 3, 4, 5, 6);
    private static final List<Integer> GUEST_TEAM = List.of(7, 8, 9, 10, 11, 12);
    private static final int WRONG_GUESS = 25;
    private static final int HOST_CORRECT_ON_GUEST = 7;
    private static final int GUEST_CORRECT_ON_HOST = 1;

    private final FriendMatchStore store = new FriendMatchStore();
    private ActiveMatchModel match;
    private Map<Integer, PokemonModel> byDex;

    @BeforeEach
    void setUp() {
        match = FriendMatchStore.newMatchShell();
        match.setGameMode(com.svc.pokeguessteam.model.enums.GameModes.FRIEND);
        match.getHostPlayer().setSide(MatchPlayerSide.HOST);
        match.getHostPlayer().setTeam(HOST_TEAM);
        match.getOpponentPlayer().setSide(MatchPlayerSide.OPPONENT);
        match.getOpponentPlayer().setTeam(GUEST_TEAM);
        match.setProfile(profile("host-profile", "host-user"));
        match.setGuestProfile(profile("guest-profile", "guest-user"));
        MatchEngine.tryStartIfBothTeamsReady(match, 6);
        store.save(match);

        byDex = Map.of(
                WRONG_GUESS, species(WRONG_GUESS, "Wrong"),
                HOST_CORRECT_ON_GUEST, species(HOST_CORRECT_ON_GUEST, "Seven"),
                GUEST_CORRECT_ON_HOST, species(GUEST_CORRECT_ON_HOST, "One")
        );
    }

    @Test
    void matchStartsActiveWithDefinedTurn() {
        assertEquals(MatchStatus.ACTIVE, match.getStatus());
        assertNotEquals(null, match.getCurrentTurn());
    }

    @Test
    void wrongGuessSwitchesTurn() {
        MatchPlayerSide starter = match.getCurrentTurn();
        applyGuess(starter, WRONG_GUESS);
        assertEquals(otherSide(starter), reloaded().getCurrentTurn());
    }

    @Test
    void correctGuessKeepsTurn() {
        MatchPlayerSide starter = match.getCurrentTurn();
        int correct = starter == MatchPlayerSide.HOST ? HOST_CORRECT_ON_GUEST : GUEST_CORRECT_ON_HOST;
        var result = applyGuess(starter, correct);
        assertEquals(GuessOutcome.KEEP_TURN, result.outcome());
        assertEquals(starter, reloaded().getCurrentTurn());
    }

    @Test
    void exclusiveLockPreventsConcurrentTurnCorruption() {
        MatchPlayerSide starter = match.getCurrentTurn();
        store.callExclusive(match.getId(), () -> {
            applyGuess(starter, WRONG_GUESS);
            return null;
        });
        MatchPlayerSide afterFirst = reloaded().getCurrentTurn();
        store.callExclusive(match.getId(), () -> {
            applyGuess(afterFirst, WRONG_GUESS);
            return null;
        });
        assertEquals(starter, reloaded().getCurrentTurn());
    }

    @Test
    void surrenderRemovesMatchFromStore() {
        MatchEngine.finishBySurrender(match, MatchPlayerSide.HOST);
        store.save(match);
        assertEquals(MatchStatus.FINISHED, match.getStatus());
        store.remove(match.getId());
        assertTrue(store.findById(match.getId()).isEmpty());
    }

    private MatchEngine.ApplyGuessResult applyGuess(MatchPlayerSide side, int dex) {
        return store.callExclusive(match.getId(), () -> {
            ActiveMatchModel current = store.findById(match.getId()).orElseThrow();
            MatchEngine.ApplyGuessResult result = MatchEngine.applyGuess(
                    current,
                    side,
                    byDex.get(dex),
                    byDex
            );
            store.save(current);
            return result;
        });
    }

    private ActiveMatchModel reloaded() {
        return store.findById(match.getId()).orElseThrow();
    }

    private static MatchPlayerSide otherSide(MatchPlayerSide side) {
        return side == MatchPlayerSide.HOST ? MatchPlayerSide.OPPONENT : MatchPlayerSide.HOST;
    }

    private static ProfileModel profile(String profileId, String userId) {
        UserModel user = new UserModel();
        setField(user, "idUser", userId);

        ProfileModel profile = new ProfileModel();
        setField(profile, "id", profileId);
        profile.setUser(user);
        return profile;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static PokemonModel species(int dex, String name) {
        PokemonModel pokemon = new PokemonModel();
        pokemon.setPokedexNumber(dex);
        pokemon.setName(name);
        return pokemon;
    }
}
