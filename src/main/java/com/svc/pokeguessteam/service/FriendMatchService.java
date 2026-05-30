package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchActionResponse;
import com.svc.pokeguessteam.dto.game.FriendMatchJoinRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.OpponentKnowledgeSlotDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.util.BotAiOpponent;
import com.svc.pokeguessteam.util.GameConstants;
import com.svc.pokeguessteam.util.JoinCodeGenerator;
import com.svc.pokeguessteam.util.MatchEngine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FriendMatchService {

    private final ActiveMatchRepository activeMatchRepository;
    private final PokemonRepository pokemonRepository;
    private final ProfileService profileService;
    private final GameHistoryService gameHistoryService;

    public FriendMatchService(
            ActiveMatchRepository activeMatchRepository,
            PokemonRepository pokemonRepository,
            ProfileService profileService,
            GameHistoryService gameHistoryService
    ) {
        this.activeMatchRepository = activeMatchRepository;
        this.pokemonRepository = pokemonRepository;
        this.profileService = profileService;
        this.gameHistoryService = gameHistoryService;
    }

    @Transactional
    public FriendMatchStateDto startMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        clearActiveFriendMatches(profile.getId());

        ActiveMatchModel match = new ActiveMatchModel();
        match.setProfile(profile);
        match.setGameMode(GameModes.FRIEND);
        match.setStatus(MatchStatus.SETUP);
        match.setJoinCode(JoinCodeGenerator.generateUnique(activeMatchRepository));

        ActiveMatchPlayerModel hostPlayer = new ActiveMatchPlayerModel();
        hostPlayer.setSide(MatchPlayerSide.USER);
        hostPlayer.setSkipTurns(0);
        match.setUserPlayer(hostPlayer);

        ActiveMatchPlayerModel guestPlayer = new ActiveMatchPlayerModel();
        guestPlayer.setSide(MatchPlayerSide.BOT);
        guestPlayer.setSkipTurns(0);
        match.setBotPlayer(guestPlayer);

        ActiveMatchModel saved = activeMatchRepository.save(match);
        return toStateDto(saved, profile, null);
    }

    @Transactional
    public FriendMatchStateDto joinMatch(String userId, FriendMatchJoinRequest request) {
        ProfileModel guestProfile = profileService.ensureProfileWithStarters(userId);
        String joinCode = JoinCodeGenerator.normalize(request.joinCode());
        if (joinCode == null || joinCode.length() < GameConstants.FRIEND_JOIN_CODE_LENGTH) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_JOIN_CODE_INVALID,
                    MessageKeys.GAME_JOIN_CODE_INVALID
            );
        }

        ActiveMatchModel match = activeMatchRepository.findByJoinCodeAndGameModeAndStatusNot(
                joinCode,
                GameModes.FRIEND,
                MatchStatus.FINISHED
        ).orElseThrow(() -> new ApiBusinessException(
                HttpStatus.NOT_FOUND,
                ErrorCodes.GAME_JOIN_CODE_NOT_FOUND,
                MessageKeys.GAME_JOIN_CODE_NOT_FOUND
        ));

        if (match.getProfile().getUser().getIdUser().equals(userId)) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_CANNOT_PLAY_SELF,
                    MessageKeys.GAME_CANNOT_PLAY_SELF
            );
        }

        if (match.getGuestProfile() != null) {
            if (match.getGuestProfile().getId().equals(guestProfile.getId())) {
                return toStateDto(match, guestProfile, null);
            }
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.GAME_MATCH_FULL,
                    MessageKeys.GAME_MATCH_FULL
            );
        }

        if (match.getStatus() != MatchStatus.SETUP) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_PHASE,
                    MessageKeys.GAME_MATCH_INVALID_PHASE
            );
        }

        clearActiveFriendMatches(guestProfile.getId());
        match.setGuestProfile(guestProfile);
        ActiveMatchModel saved = activeMatchRepository.save(match);
        return toStateDto(saved, guestProfile, null);
    }

    @Transactional(readOnly = true)
    public FriendMatchStateDto getActiveMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveFriendMatch(profile);
        return toStateDto(match, profile, null);
    }

    @Transactional
    public FriendMatchActionResponse submitTeam(String userId, BotMatchTeamRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveFriendMatch(profile);
        ensureGuestJoined(match);

        if (match.getStatus() != MatchStatus.SETUP && match.getStatus() != MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_PHASE,
                    MessageKeys.GAME_MATCH_INVALID_PHASE
            );
        }
        if (match.getStatus() == MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_PHASE,
                    MessageKeys.GAME_MATCH_TEAM_LOCKED
            );
        }

        MatchPlayerSide side = resolveSide(match, profile);
        List<Integer> team = validateTeam(request.team());
        getPlayer(match, side).setTeam(team);

        MatchEngine.tryStartIfBothTeamsReady(match, GameConstants.TEAM_SIZE);
        activeMatchRepository.save(match);

        return new FriendMatchActionResponse(toStateDto(match, profile, null), List.of());
    }

    @Transactional
    public FriendMatchActionResponse submitGuess(String userId, BotMatchGuessRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveFriendMatch(profile);
        ensureGuestJoined(match);

        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }

        MatchPlayerSide side = resolveSide(match, profile);
        if (match.getCurrentTurn() != side) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_WRONG_TURN,
                    MessageKeys.GAME_MATCH_WRONG_TURN
            );
        }

        PokemonModel guessed = requirePokemon(request.pokedexNumber());
        ensureNotAlreadyGuessed(match, side, guessed.getPokedexNumber());

        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        MatchEngine.ApplyGuessResult result = applyGuessSafe(match, side, guessed, pokemonByDex);

        activeMatchRepository.save(match);
        GameHistoryEntryDto history = finalizeIfFinished(match, null);

        List<BotMatchGuessFeedbackDto> feedbacks = List.of(toFeedback(result, pokemonByDex));
        return new FriendMatchActionResponse(toStateDto(match, profile, history), feedbacks);
    }

    @Transactional
    public FriendMatchActionResponse surrender(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveFriendMatch(profile);
        ensureGuestJoined(match);

        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }

        MatchPlayerSide side = resolveSide(match, profile);
        MatchEngine.finishBySurrender(match, side);
        activeMatchRepository.save(match);
        GameHistoryEntryDto history = finalizeIfFinished(match, side);

        return new FriendMatchActionResponse(toStateDto(match, profile, history), List.of());
    }

    @Transactional
    public void abandonMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = activeMatchRepository.findActiveFriendMatchForProfile(
                profile.getId(),
                GameModes.FRIEND,
                MatchStatus.FINISHED
        ).orElseThrow(() -> new ApiBusinessException(
                HttpStatus.NOT_FOUND,
                ErrorCodes.GAME_FRIEND_MATCH_NOT_FOUND,
                MessageKeys.GAME_FRIEND_MATCH_NOT_FOUND
        ));

        if (match.getProfile().getId().equals(profile.getId())) {
            activeMatchRepository.delete(match);
            return;
        }

        if (match.getStatus() == MatchStatus.ACTIVE) {
            surrender(userId);
            return;
        }

        match.setGuestProfile(null);
        match.getBotPlayer().setTeam(new ArrayList<>());
        activeMatchRepository.save(match);
    }

    private GameHistoryEntryDto finalizeIfFinished(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        if (match.getStatus() != MatchStatus.FINISHED) {
            return null;
        }
        return gameHistoryService.saveFriendGameFromActiveMatch(match, surrenderSide);
    }

    private FriendMatchStateDto toStateDto(
            ActiveMatchModel match,
            ProfileModel viewerProfile,
            GameHistoryEntryDto history
    ) {
        MatchPlayerSide viewerSide = resolveSide(match, viewerProfile);
        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        List<OpponentKnowledgeSlotDto> knowledge = BotAiOpponent
                .buildOpponentKnowledge(match, viewerSide, pokemonByDex)
                .stream()
                .map(OpponentKnowledgeSlotDto::from)
                .toList();

        List<BotMatchGuessFeedbackDto> recentGuesses = match.getGuesses().stream()
                .limit(20)
                .map(g -> {
                    PokemonModel p = pokemonByDex.get(g.getGuessedPokedexNumber());
                    String name = p != null ? p.getName() : String.valueOf(g.getGuessedPokedexNumber());
                    return BotMatchGuessFeedbackDto.from(g, name, null, null);
                })
                .toList();

        return FriendMatchStateDto.from(match, viewerSide, knowledge, recentGuesses, history);
    }

    private ActiveMatchModel requireActiveFriendMatch(ProfileModel profile) {
        return activeMatchRepository.findActiveFriendMatchForProfile(
                profile.getId(),
                GameModes.FRIEND,
                MatchStatus.FINISHED
        ).orElseThrow(() -> new ApiBusinessException(
                HttpStatus.NOT_FOUND,
                ErrorCodes.GAME_FRIEND_MATCH_NOT_FOUND,
                MessageKeys.GAME_FRIEND_MATCH_NOT_FOUND
        ));
    }

    private void clearActiveFriendMatches(String profileId) {
        activeMatchRepository.findActiveFriendMatchForProfile(
                profileId,
                GameModes.FRIEND,
                MatchStatus.FINISHED
        ).ifPresent(activeMatchRepository::delete);
    }

    private static void ensureGuestJoined(ActiveMatchModel match) {
        if (match.getGuestProfile() == null) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_GUEST_REQUIRED,
                    MessageKeys.GAME_MATCH_GUEST_REQUIRED
            );
        }
    }

    private static MatchPlayerSide resolveSide(ActiveMatchModel match, ProfileModel profile) {
        if (match.getProfile().getId().equals(profile.getId())) {
            return MatchPlayerSide.USER;
        }
        if (match.getGuestProfile() != null && match.getGuestProfile().getId().equals(profile.getId())) {
            return MatchPlayerSide.BOT;
        }
        throw new ApiBusinessException(
                HttpStatus.FORBIDDEN,
                ErrorCodes.GAME_MATCH_NOT_FOUND,
                MessageKeys.GAME_FRIEND_MATCH_NOT_FOUND
        );
    }

    private static ActiveMatchPlayerModel getPlayer(ActiveMatchModel match, MatchPlayerSide side) {
        return side == MatchPlayerSide.USER ? match.getUserPlayer() : match.getBotPlayer();
    }

    private MatchEngine.ApplyGuessResult applyGuessSafe(
            ActiveMatchModel match,
            MatchPlayerSide side,
            PokemonModel guessed,
            Map<Integer, PokemonModel> pokemonByDex
    ) {
        try {
            return MatchEngine.applyGuess(match, side, guessed, pokemonByDex);
        } catch (IllegalStateException ex) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_ACTION,
                    ex.getMessage()
            );
        }
    }

    private BotMatchGuessFeedbackDto toFeedback(
            MatchEngine.ApplyGuessResult result,
            Map<Integer, PokemonModel> pokemonByDex
    ) {
        PokemonModel guessed = pokemonByDex.get(result.guess().getGuessedPokedexNumber());
        String name = guessed != null ? guessed.getName() : String.valueOf(result.guess().getGuessedPokedexNumber());
        return BotMatchGuessFeedbackDto.from(result.guess(), name, result.outcome(), result.message());
    }

    private List<Integer> validateTeam(List<Integer> team) {
        if (team == null || team.size() != GameConstants.TEAM_SIZE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_TEAM_INVALID,
                    MessageKeys.GAME_TEAM_INVALID,
                    GameConstants.TEAM_SIZE
            );
        }
        Set<Integer> unique = new HashSet<>(team);
        if (unique.size() != GameConstants.TEAM_SIZE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_TEAM_INVALID,
                    MessageKeys.GAME_TEAM_DUPLICATE
            );
        }
        for (Integer dex : team) {
            if (pokemonRepository.findByPokedexNumber(dex).isEmpty()) {
                throw new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.GAME_TEAM_INVALID,
                        MessageKeys.POKEMON_SPECIES_NOT_FOUND
                );
            }
        }
        return List.copyOf(team);
    }

    private PokemonModel requirePokemon(int pokedexNumber) {
        return pokemonRepository.findByPokedexNumber(pokedexNumber)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.POKEMON_SPECIES_NOT_FOUND,
                        MessageKeys.POKEMON_SPECIES_NOT_FOUND
                ));
    }

    private void ensureNotAlreadyGuessed(ActiveMatchModel match, MatchPlayerSide side, int pokedexNumber) {
        boolean already = match.getGuesses().stream()
                .anyMatch(g -> g.getPlayerSide() == side && g.getGuessedPokedexNumber() == pokedexNumber);
        if (already) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_GUESS_ALREADY_USED,
                    MessageKeys.GAME_GUESS_ALREADY_USED
            );
        }
    }

    private Map<Integer, PokemonModel> loadPokemonByDex() {
        return pokemonRepository.findAllByOrderByPokedexNumberAsc().stream()
                .collect(Collectors.toMap(PokemonModel::getPokedexNumber, Function.identity(), (a, b) -> a));
    }
}
