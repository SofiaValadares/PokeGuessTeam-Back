package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.dto.game.TeamOpeningDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.enums.GuessOutcome;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonType;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.service.ActiveMatchConstraintService;
import com.svc.pokeguessteam.service.CryptoService;
import com.svc.pokeguessteam.service.FriendMatchStore;
import com.svc.pokeguessteam.service.TeamCommitmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras da partida: finish, placar, recompensa, HMAC, motor de palpites e fluxo amigo.
 */
class MatchIntegrityRulesTest {

    @Nested
    class BotFinishResolver {

        @Test
        void surrenderIsAllowedOnlyWhileTheMatchIsActive() {
            assertEquals(
                    GameResults.DESISTENCE,
                    BotMatchFinishResolver.resolve(MatchStatus.ACTIVE, null, GameResults.DESISTENCE)
            );
        }

        @ParameterizedTest
        @EnumSource(value = GameResults.class, names = {"WIN", "LOSE", "DRAW"})
        void unfinishedMatchCannotClaimACompetitiveResult(GameResults claimed) {
            ApiBusinessException ex = assertThrows(
                    ApiBusinessException.class,
                    () -> BotMatchFinishResolver.resolve(MatchStatus.ACTIVE, null, claimed)
            );
            assertEquals(ErrorCodes.GAME_MATCH_INVALID_PHASE, ex.getCode());
        }

        @Test
        void finishedMatchIgnoresTheClientClaimAndUsesTheServerWinner() {
            assertEquals(
                    GameResults.LOSE,
                    BotMatchFinishResolver.resolve(MatchStatus.FINISHED, MatchPlayerSide.OPPONENT, GameResults.WIN)
            );
            assertEquals(
                    GameResults.WIN,
                    BotMatchFinishResolver.resolve(MatchStatus.FINISHED, MatchPlayerSide.HOST, GameResults.DESISTENCE)
            );
            assertEquals(
                    GameResults.DRAW,
                    BotMatchFinishResolver.resolve(MatchStatus.FINISHED, null, GameResults.WIN)
            );
        }
    }

    @Nested
    class ScoreAndResultConsistency {

        @Test
        void opponentResultInvertsWinLoseAndTurnsDesistenceIntoAWin() {
            assertEquals(GameResults.LOSE, GameFinishValidation.opponentResult(GameResults.WIN));
            assertEquals(GameResults.WIN, GameFinishValidation.opponentResult(GameResults.LOSE));
            assertEquals(GameResults.DRAW, GameFinishValidation.opponentResult(GameResults.DRAW));
            assertEquals(GameResults.WIN, GameFinishValidation.opponentResult(GameResults.DESISTENCE));
        }

        @ParameterizedTest
        @CsvSource({
                "4, 2, WIN",
                "1, 6, LOSE",
                "3, 3, DRAW"
        })
        void acceptsScoresThatMatchTheClaimedResult(int user, int opponent, GameResults result) {
            GameFinishValidation.validateResult(user, opponent, result);
        }

        @Test
        void rejectsInconsistentOrInvalidScoresAndAllowsDesistenceWithAnyScore() {
            ApiBusinessException win = assertThrows(
                    ApiBusinessException.class,
                    () -> GameFinishValidation.validateResult(2, 4, GameResults.WIN)
            );
            assertEquals(ErrorCodes.GAME_RESULT_INCONSISTENT, win.getCode());

            ApiBusinessException negative = assertThrows(
                    ApiBusinessException.class,
                    () -> GameFinishValidation.validateResult(-1, 0, GameResults.WIN)
            );
            assertEquals(ErrorCodes.GAME_CORRECT_GUESSES_INVALID, negative.getCode());

            ApiBusinessException missing = assertThrows(
                    ApiBusinessException.class,
                    () -> GameFinishValidation.validateResult(4, 2, null)
            );
            assertEquals(ErrorCodes.GAME_RESULT_REQUIRED, missing.getCode());

            GameFinishValidation.validateResult(0, 6, GameResults.DESISTENCE);
            GameFinishValidation.validateResult(6, 0, GameResults.DESISTENCE);
        }

        @Test
        void localOpponentNameMustBeBetweenThreeAndOneHundredTwentyChars() {
            assertEquals("Red", GameFinishValidation.validateAndNormalizeLocalOpponentName("  Red  "));
            assertThrows(ApiBusinessException.class, () -> GameFinishValidation.validateLocalOpponentName("ab"));
            assertThrows(ApiBusinessException.class, () -> GameFinishValidation.validateLocalOpponentName("  "));
            assertThrows(
                    ApiBusinessException.class,
                    () -> GameFinishValidation.validateLocalOpponentName("x".repeat(121))
            );
        }
    }

    @Nested
    class Rewards {

        @Test
        void friendWinPaysMoreThanALossAndBotWinPaysLessThanFriendWin() {
            var friendWin = GameMatchRewards.payout(GameModes.FRIEND, GameResults.WIN);
            var friendLose = GameMatchRewards.payout(GameModes.FRIEND, GameResults.LOSE);
            var friendDraw = GameMatchRewards.payout(GameModes.FRIEND, GameResults.DRAW);
            var botWin = GameMatchRewards.payout(GameModes.BOT, GameResults.WIN);
            var localWin = GameMatchRewards.payout(GameModes.LOCAL, GameResults.WIN);
            var competitiveWin = GameMatchRewards.payout(GameModes.COMPETITIVE, GameResults.WIN);

            assertEquals(300, friendWin.trainingTeamXp());
            assertEquals(1, friendWin.pokeBalls());
            assertEquals(0, friendWin.pokeballFragments());
            assertEquals(150, friendLose.trainingTeamXp());
            assertEquals(5, friendLose.pokeballFragments());
            assertEquals(friendLose, friendDraw);
            assertEquals(friendWin, competitiveWin);

            assertEquals(150, botWin.trainingTeamXp());
            assertEquals(5, botWin.pokeballFragments());
            assertEquals(botWin, localWin);
        }

        @ParameterizedTest
        @EnumSource(value = GameResults.class, names = {"LOSE", "DRAW", "DESISTENCE"})
        void botParticipationResultsShareTheSamePayout(GameResults result) {
            var payout = GameMatchRewards.payout(GameModes.BOT, result);
            assertEquals(75, payout.trainingTeamXp());
            assertEquals(0, payout.pokeBalls());
            assertEquals(0, payout.pokeballFragments());
        }
    }

    @Nested
    class TeamCommitments {

        private static final String KEY =
                "617e30427ecf9bf102367d03beb131336224bc1da1584ff3d04a50aa11422894";

        private TeamCommitmentService service;

        @BeforeEach
        void setUp() {
            service = new TeamCommitmentService(new CryptoService(KEY));
        }

        @Test
        void honestOpeningRoundTripsAndAMutatedTeamNonceOrMacIsRejected() {
            List<Integer> team = List.of(25, 6, 9, 3, 94, 130);
            TeamCommitmentService.IssuedTeamCommitment issued = service.issue(team);
            ActiveMatchPlayerModel player = bound(issued);

            TeamOpeningDto opened = service.openClaimedTeam(player, team, null);
            assertEquals(team, opened.team());
            assertEquals(issued.nonce(), opened.nonce());
            assertEquals(64, issued.commitment().length());
            assertEquals(issued.nonce(), service.nonceOf(player));
            service.verifyOpening(player, new TeamOpeningDto(team, issued.nonce()));

            assertThrows(
                    ApiBusinessException.class,
                    () -> service.openClaimedTeam(player, List.of(1, 2, 3, 4, 5, 7), null)
            );

            TeamCommitmentService.IssuedTeamCommitment other = service.issue(team);
            assertNotEquals(issued.nonce(), other.nonce());
            assertThrows(
                    ApiBusinessException.class,
                    () -> service.verifyOpening(player, new TeamOpeningDto(team, other.nonce()))
            );

            player.setTeamCommitment("aa".repeat(32));
            assertThrows(ApiBusinessException.class, () -> service.openSealed(player));
        }

        @Test
        void canonicalPayloadIsStableAndAesRoundTrips() {
            String payload = TeamCommitmentService.canonicalPayload(List.of(1, 2, 3, 4, 5, 6), "ab".repeat(32));
            assertTrue(payload.startsWith("v1|1,2,3,4,5,6|"));
            CryptoService crypto = new CryptoService(KEY);
            assertEquals(64, crypto.sha256Hex(payload).length());
            assertEquals(payload, crypto.decrypt(crypto.encrypt(payload)));
        }

        private static ActiveMatchPlayerModel bound(TeamCommitmentService.IssuedTeamCommitment issued) {
            ActiveMatchPlayerModel player = new ActiveMatchPlayerModel();
            player.setTeamCommitment(issued.commitment());
            player.setTeamCommitmentMac(issued.mac());
            player.setSealedOpening(issued.sealedOpening());
            return player;
        }
    }

    @Nested
    class MatchEngineTurns {

        private ActiveMatchModel match;
        private Map<Integer, PokemonModel> byDex;

        @BeforeEach
        void setUp() {
            match = new ActiveMatchModel();
            match.setStatus(MatchStatus.ACTIVE);
            match.setCurrentTurn(MatchPlayerSide.HOST);

            ActiveMatchPlayerModel user = new ActiveMatchPlayerModel();
            user.setSide(MatchPlayerSide.HOST);
            user.setTeam(List.of(1, 4, 7, 25, 133, 150));
            match.setHostPlayer(user);

            ActiveMatchPlayerModel bot = new ActiveMatchPlayerModel();
            bot.setSide(MatchPlayerSide.OPPONENT);
            bot.setTeam(List.of(2, 5, 8, 26, 134, 151));
            match.setOpponentPlayer(bot);

            byDex = Map.of(
                    25, species(25, "Pikachu"),
                    26, species(26, "Raichu"),
                    2, species(2, "Ivysaur")
            );
        }

        @Test
        void correctGuessMatchesByPokedexNumberEvenWhenTheNameDiffersAndKeepsTheTurn() {
            PokemonModel bulbasaurEn = species(1, "Bulbasaur");
            byDex = Map.of(1, bulbasaurEn, 26, species(26, "Raichu"));
            match.getOpponentPlayer().setTeam(List.of(1, 2, 3, 4, 5, 6));

            var aliased = MatchEngine.applyGuess(match, MatchPlayerSide.HOST, species(1, "Bulbasauro"), byDex);
            assertTrue(aliased.guess().isExactMatch());
            assertEquals(GuessOutcome.KEEP_TURN, aliased.outcome());
            assertTrue(match.getHostPlayer().getHits().contains(1));

            match.getOpponentPlayer().setTeam(List.of(2, 5, 8, 26, 134, 151));
            byDex = Map.of(26, species(26, "Raichu"), 25, species(25, "Pikachu"));
            var keep = MatchEngine.applyGuess(match, MatchPlayerSide.HOST, byDex.get(26), byDex);
            assertEquals(GuessOutcome.KEEP_TURN, keep.outcome());
            assertEquals(MatchPlayerSide.HOST, match.getCurrentTurn());
            assertEquals(2, match.getHostPlayer().getHits().size());
        }

        @Test
        void wrongGuessSwitchesTurn() {
            var result = MatchEngine.applyGuess(match, MatchPlayerSide.HOST, byDex.get(25), byDex);
            assertEquals(GuessOutcome.SWITCH_TURN, result.outcome());
            assertEquals(MatchPlayerSide.OPPONENT, match.getCurrentTurn());
        }

        private static PokemonModel species(int dex, String name) {
            PokemonModel pokemon = new PokemonModel();
            pokemon.setPokedexNumber(dex);
            pokemon.setName(name);
            pokemon.setPrimaryType(PokemonType.NORMAL);
            pokemon.setGeneration(1);
            pokemon.setColor(PokedexColor.YELLOW);
            pokemon.setHeightM(1.0);
            pokemon.setWeightKg(10.0);
            return pokemon;
        }
    }

    @Nested
    class FriendMatchFlow {

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
            match.setGameMode(GameModes.FRIEND);
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
        void startsActiveAndKeepsOrSwitchesTurnAccordingToTheGuess() {
            assertEquals(MatchStatus.ACTIVE, match.getStatus());
            assertNotNull(match.getCurrentTurn());

            MatchPlayerSide starter = match.getCurrentTurn();
            int correct = starter == MatchPlayerSide.HOST ? HOST_CORRECT_ON_GUEST : GUEST_CORRECT_ON_HOST;
            var hit = applyGuess(starter, correct);
            assertEquals(GuessOutcome.KEEP_TURN, hit.outcome());
            assertEquals(starter, reloaded().getCurrentTurn());

            applyGuess(starter, WRONG_GUESS);
            assertEquals(otherSide(starter), reloaded().getCurrentTurn());
        }

        @Test
        void exclusiveLockSerializesGuessesOnTheSameMatch() {
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
        void surrenderRemovesTheMatchFromTheStore() {
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

    @Nested
    class OneMatchAtATime {

        @Test
        void blocksAnUnfinishedPersistedMatchOrAnActiveFriendRoom() {
            ActiveMatchModel existing = new ActiveMatchModel();
            existing.setGameMode(GameModes.LOCAL);
            existing.setStatus(MatchStatus.SETUP);

            ActiveMatchConstraintService service = constraintService(List.of(existing));
            ApiBusinessException persisted = assertThrows(
                    ApiBusinessException.class,
                    () -> service.ensureCanStartNewMatch("profile-1")
            );
            assertEquals(ErrorCodes.GAME_MATCH_ALREADY_IN_PROGRESS, persisted.getCode());

            FriendMatchStore store = new FriendMatchStore();
            ActiveMatchConstraintService emptyDb = constraintService(List.of(), store);
            assertDoesNotThrow(() -> emptyDb.ensureCanStartNewMatch("host-profile"));

            ActiveMatchModel room = FriendMatchStore.newMatchShell();
            room.setGameMode(GameModes.FRIEND);
            room.setStatus(MatchStatus.SETUP);
            room.setProfile(profile("host-profile", "host-user"));
            store.save(room);
            ApiBusinessException friend = assertThrows(
                    ApiBusinessException.class,
                    () -> emptyDb.ensureCanStartNewMatch("host-profile")
            );
            assertEquals(ErrorCodes.GAME_MATCH_ALREADY_IN_PROGRESS, friend.getCode());
        }

        private static ActiveMatchConstraintService constraintService(List<ActiveMatchModel> unfinished) {
            return constraintService(unfinished, new FriendMatchStore());
        }

        private static ActiveMatchConstraintService constraintService(
                List<ActiveMatchModel> unfinished,
                FriendMatchStore store
        ) {
            ActiveMatchRepository repository = (ActiveMatchRepository) Proxy.newProxyInstance(
                    ActiveMatchRepository.class.getClassLoader(),
                    new Class<?>[] { ActiveMatchRepository.class },
                    (proxy, method, args) -> {
                        if ("findAllUnfinishedForProfileOrderByCreatedAtDesc".equals(method.getName())) {
                            return unfinished;
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
            );
            return new ActiveMatchConstraintService(repository, store, null);
        }

        private static ProfileModel profile(String profileId, String userId) {
            UserModel user = new UserModel();
            setField(user, "idUser", userId);
            ProfileModel model = new ProfileModel();
            setField(model, "id", profileId);
            model.setUser(user);
            return model;
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
    }
}
