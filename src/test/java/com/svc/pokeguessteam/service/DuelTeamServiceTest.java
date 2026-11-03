package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokemonInventoryRepository;
import com.svc.pokeguessteam.util.GameConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class DuelTeamServiceTest {

    private static final String USER_ID = "user-1";
    private static final List<Integer> TEAM = List.of(1, 4, 7, 25, 94, 130);
    private static final Set<Integer> REGISTERED = Set.of(1, 4, 7, 25, 94, 130, 6, 9, 3, 143, 149, 150);

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserPokemonInventoryRepository inventoryRepository;

    private StubCatalog catalog;
    private DuelTeamService service;

    @BeforeEach
    void setUp() {
        catalog = new StubCatalog(REGISTERED);
        service = new DuelTeamService(
                new StubUserPokedexService(new HashSet<>(REGISTERED)),
                catalog,
                profileRepository,
                inventoryRepository
        );
    }

    @Nested
    class TeamValidation {

        @Test
        void acceptsSixUniqueRegisteredSpeciesAndReusesTheSameSet() {
            Set<Integer> registered = service.registeredNumbers(USER_ID);
            assertEquals(TEAM, service.validateTeam(registered, TEAM, null));
            assertEquals(TEAM, service.validateTeamFromRegisteredPokedex(USER_ID, TEAM));
            assertEquals(REGISTERED.size(), registered.size());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.svc.pokeguessteam.service.DuelTeamServiceTest#invalidTeams")
        void rejectsMalformedOrUnownedTeams(String label, List<Integer> team, String expectedCode) {
            ApiBusinessException ex = assertThrows(
                    ApiBusinessException.class,
                    () -> service.validateTeamFromRegisteredPokedex(USER_ID, team)
            );
            assertEquals(expectedCode, ex.getCode(), label);
        }

        @Test
        void rejectsUnknownSpeciesEvenIfRegisteredNumberIsListed() {
            catalog.remove(130);
            ApiBusinessException ex = assertThrows(
                    ApiBusinessException.class,
                    () -> service.validateTeamFromRegisteredPokedex(USER_ID, TEAM)
            );
            assertEquals(ErrorCodes.GAME_TEAM_INVALID, ex.getCode());
        }

        @Test
        void rejectsWhenTheRegisteredPokedexHasFewerThanSixSpecies() {
            DuelTeamService tiny = new DuelTeamService(
                    new StubUserPokedexService(Set.of(1, 4, 7)),
                    catalog,
                    profileRepository,
                    inventoryRepository
            );
            ApiBusinessException ex = assertThrows(
                    ApiBusinessException.class,
                    () -> tiny.validateTeamFromRegisteredPokedex(USER_ID, TEAM)
            );
            assertEquals(ErrorCodes.GAME_POKEDEX_INSUFFICIENT, ex.getCode());
        }

        @Test
        void eventPoolMustContainEveryMemberAndBeLargeEnough() {
            Set<Integer> tooSmall = Set.of(1, 4, 7);
            ApiBusinessException insufficient = assertThrows(
                    ApiBusinessException.class,
                    () -> service.validateTeamFromRegisteredPokedex(USER_ID, TEAM, tooSmall)
            );
            assertEquals(ErrorCodes.GAME_EVENT_POOL_INSUFFICIENT, insufficient.getCode());

            Set<Integer> withoutGyarados = Set.of(1, 4, 7, 25, 94, 6, 9, 3, 143, 149, 150);
            ApiBusinessException outside = assertThrows(
                    ApiBusinessException.class,
                    () -> service.validateTeamFromRegisteredPokedex(USER_ID, TEAM, withoutGyarados)
            );
            assertEquals(ErrorCodes.GAME_TEAM_NOT_IN_EVENT, outside.getCode());
        }
    }

    @Nested
    class GuessAndBotPool {

        @Test
        void guessMustBeInTheRegisteredPokedex() {
            service.requireRegisteredGuess(USER_ID, 25);
            ApiBusinessException ex = assertThrows(
                    ApiBusinessException.class,
                    () -> service.requireRegisteredGuess(USER_ID, 999)
            );
            assertEquals(ErrorCodes.GAME_GUESS_NOT_IN_POKEDEX, ex.getCode());
        }

        @Test
        void registeredModelsComeFromTheInMemoryCatalog() {
            List<PokemonModel> models = service.modelsFromRegistered(REGISTERED);
            assertEquals(REGISTERED.size(), models.size());
            assertEquals(1, models.get(0).getPokedexNumber());
            assertTrue(service.modelsFromRegistered(Set.of()).isEmpty());
        }

        @Test
        void botTeamAvoidsTheHostAndStaysInsideTheRegisteredPool() {
            Set<Integer> excluded = new HashSet<>(TEAM);
            List<Integer> botTeam = service.buildBotTeamFromRegistered(REGISTERED, excluded);
            assertEquals(GameConstants.TEAM_SIZE, botTeam.size());
            assertEquals(Set.copyOf(botTeam).size(), botTeam.size());
            assertTrue(REGISTERED.containsAll(botTeam));
            excluded.forEach(dex -> assertFalse(botTeam.contains(dex)));
        }

        @Test
        void botTeamFailsWhenTheRemainingPoolIsTooSmall() {
            ApiBusinessException ex = assertThrows(
                    ApiBusinessException.class,
                    () -> service.buildBotTeamFromRegistered(Set.copyOf(TEAM), Set.copyOf(TEAM))
            );
            assertEquals(ErrorCodes.GAME_POKEDEX_INSUFFICIENT, ex.getCode());
        }
    }

    static Stream<Arguments> invalidTeams() {
        return Stream.of(
                Arguments.of("null", null, ErrorCodes.GAME_TEAM_INVALID),
                Arguments.of("too small", List.of(1, 4, 7), ErrorCodes.GAME_TEAM_INVALID),
                Arguments.of("duplicate", List.of(1, 4, 7, 25, 94, 1), ErrorCodes.GAME_TEAM_INVALID),
                Arguments.of("not owned", List.of(1, 4, 7, 25, 94, 999), ErrorCodes.GAME_TEAM_NOT_IN_POKEDEX)
        );
    }

    private static PokemonModel species(int dex) {
        PokemonModel pokemon = new PokemonModel();
        pokemon.setPokedexNumber(dex);
        pokemon.setName("P" + dex);
        return pokemon;
    }

    private static final class StubUserPokedexService extends UserPokedexService {
        private final Set<Integer> registered;

        private StubUserPokedexService(Set<Integer> registered) {
            super(null, null, null, null);
            this.registered = registered;
        }

        @Override
        public Set<Integer> findRegisteredPokedexNumbers(String userId) {
            return registered;
        }
    }

    private static final class StubCatalog extends NationalPokedexCatalog {
        private final Map<Integer, PokemonModel> byDex = new LinkedHashMap<>();

        private StubCatalog(Set<Integer> dexNumbers) {
            super(null);
            dexNumbers.forEach(dex -> byDex.put(dex, species(dex)));
        }

        private void remove(int dex) {
            byDex.remove(dex);
        }

        @Override
        public Map<Integer, PokemonModel> speciesByDex() {
            return byDex;
        }
    }
}
