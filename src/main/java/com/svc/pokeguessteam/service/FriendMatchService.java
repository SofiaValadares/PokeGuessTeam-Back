package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchActionResponse;
import com.svc.pokeguessteam.dto.game.FriendMatchJoinRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.MatchRewardDto;
import com.svc.pokeguessteam.dto.game.OpponentSlotKnowledgeDto;
import com.svc.pokeguessteam.dto.game.OpponentTeamKnowledgeResponse;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.util.GameConstants;
import com.svc.pokeguessteam.util.JoinCodeGenerator;
import com.svc.pokeguessteam.util.MatchEngine;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FriendMatchService {

    private final PokemonRepository pokemonRepository;
    private final ProfileService profileService;
    private final GameHistoryService gameHistoryService;
    private final MatchRewardService matchRewardService;
    private final MatchKnowledgeService matchKnowledgeService;
    private final ActiveMatchConstraintService activeMatchConstraintService;
    private final FriendMatchTurnCoordinator friendMatchTurnCoordinator;
    private final DuelTeamService duelTeamService;
    private final FriendMatchStore friendMatchStore;

    public FriendMatchService(
            PokemonRepository pokemonRepository,
            ProfileService profileService,
            GameHistoryService gameHistoryService,
            MatchRewardService matchRewardService,
            MatchKnowledgeService matchKnowledgeService,
            ActiveMatchConstraintService activeMatchConstraintService,
            @Lazy FriendMatchTurnCoordinator friendMatchTurnCoordinator,
            DuelTeamService duelTeamService,
            FriendMatchStore friendMatchStore
    ) {
        this.pokemonRepository = pokemonRepository;
        this.profileService = profileService;
        this.gameHistoryService = gameHistoryService;
        this.matchRewardService = matchRewardService;
        this.matchKnowledgeService = matchKnowledgeService;
        this.activeMatchConstraintService = activeMatchConstraintService;
        this.friendMatchTurnCoordinator = friendMatchTurnCoordinator;
        this.duelTeamService = duelTeamService;
        this.friendMatchStore = friendMatchStore;
    }

    public record TurnTimeoutStep(
            FriendMatchStateDto hostView,
            FriendMatchStateDto guestView,
            BotMatchGuessFeedbackDto feedback
    ) {
    }

    @Transactional
    public FriendMatchStateDto startMatch(String userId, BotMatchTeamRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        activeMatchConstraintService.clearStaleClientSideMatches(profile.getId());
        activeMatchConstraintService.ensureCanStartNewMatch(profile.getId());

        List<Integer> team = duelTeamService.validateTeamFromRegisteredPokedex(userId, request.team());

        ActiveMatchModel match = FriendMatchStore.newMatchShell();
        match.setProfile(profile);
        match.setGameMode(GameModes.FRIEND);
        match.setJoinCode(JoinCodeGenerator.generateUnique(friendMatchStore::isJoinCodeTaken));
        match.getHostPlayer().setSide(MatchPlayerSide.HOST);
        match.getHostPlayer().setSkipTurns(0);
        match.getHostPlayer().setTeam(team);
        match.getOpponentPlayer().setSide(MatchPlayerSide.OPPONENT);
        match.getOpponentPlayer().setSkipTurns(0);

        ActiveMatchModel saved = saveMatch(match);
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

        ActiveMatchModel match = friendMatchStore.findByJoinCode(joinCode)
                .orElseThrow(() -> new ApiBusinessException(
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

        List<Integer> team = duelTeamService.validateTeamFromRegisteredPokedex(userId, request.team());

        activeMatchConstraintService.clearStaleClientSideMatches(guestProfile.getId());
        activeMatchConstraintService.ensureCanStartNewMatch(guestProfile.getId());
        match.setGuestProfile(guestProfile);
        match.getOpponentPlayer().setTeam(team);
        MatchEngine.tryStartIfBothTeamsReady(match, GameConstants.TEAM_SIZE);
        ActiveMatchModel saved = saveMatch(match);

        String hostUserId = saved.getProfile().getUser().getIdUser();
        String guestUserId = guestProfile.getUser().getIdUser();
        if (saved.getStatus() == MatchStatus.ACTIVE) {
            friendMatchTurnCoordinator.afterTeamReady(
                    saved.getId(),
                    hostUserId,
                    guestUserId,
                    toStateDto(saved, saved.getProfile(), null)
            );
        }
        return toStateDto(saved, guestProfile, null);
    }

    @Transactional(readOnly = true)
    public OpponentTeamKnowledgeResponse getOpponentKnowledge(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveFriendMatch(profile);
        return matchKnowledgeService.getOpponentKnowledgeForCurrentTurn(match);
    }

    @Transactional
    public FriendMatchActionResponse submitTeam(String userId, BotMatchTeamRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveFriendMatch(profile);
        MatchPlayerSide side = resolveSide(match, profile);
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

        List<Integer> team = duelTeamService.validateTeamFromRegisteredPokedex(userId, request.team());
        ActiveMatchPlayerModel player = getPlayer(match, side);
        if (player.getTeam().size() >= GameConstants.TEAM_SIZE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_PHASE,
                    MessageKeys.GAME_MATCH_TEAM_LOCKED
            );
        }
        player.setTeam(team);

        MatchEngine.tryStartIfBothTeamsReady(match, GameConstants.TEAM_SIZE);
        saveMatch(match);

        FriendMatchStateDto viewerState = toStateDto(match, profile, null);
        if (match.getGuestProfile() != null) {
            String hostUserId = match.getProfile().getUser().getIdUser();
            String guestUserId = match.getGuestProfile().getUser().getIdUser();
            friendMatchTurnCoordinator.afterTeamReady(
                    match.getId(),
                    hostUserId,
                    guestUserId,
                    toStateDto(match, match.getProfile(), null)
            );
        }
        return new FriendMatchActionResponse(viewerState, List.of());
    }

    @Transactional
    public FriendMatchActionResponse submitGuess(String userId, BotMatchGuessRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        String matchId = requireActiveFriendMatch(profile).getId();
        return friendMatchStore.callExclusive(matchId, () -> submitGuessLocked(userId, request, profile, matchId));
    }

    private FriendMatchActionResponse submitGuessLocked(
            String userId,
            BotMatchGuessRequest request,
            ProfileModel profile,
            String matchId
    ) {
        ActiveMatchModel match = requireMatchById(matchId);
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

        saveMatch(match);
        GameHistoryEntryDto history = finalizeIfFinished(match, null);

        List<BotMatchGuessFeedbackDto> feedbacks = List.of(toFeedback(result, pokemonByDex));
        FriendMatchStateDto state = toStateDto(match, profile, history, null);
        String hostUserId = match.getProfile().getUser().getIdUser();
        String guestUserId = match.getGuestProfile().getUser().getIdUser();
        FriendMatchStateDto hostView = toStateDto(match, match.getProfile(), history, null);
        FriendMatchStateDto guestView = toStateDto(match, match.getGuestProfile(), history, null);

        MatchRewardDto reward = null;
        if (match.getStatus() == MatchStatus.FINISHED) {
            reward = completeIfFinished(match, null, userId);
            state = toStateDto(match, profile, history, null);
            hostView = toStateDto(match, match.getProfile(), history, null);
            guestView = toStateDto(match, match.getGuestProfile(), history, null);
        }

        friendMatchTurnCoordinator.afterHumanGuess(match.getId(), hostUserId, guestUserId, hostView);
        return new FriendMatchActionResponse(state, feedbacks, reward);
    }

    @Transactional(readOnly = true)
    public Optional<FriendMatchStateDto> findActiveMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        return friendMatchStore.findActiveForProfile(profile.getId())
                .flatMap(match -> {
                    if (match.getStatus() == MatchStatus.FINISHED) {
                        friendMatchStore.remove(match.getId());
                        return Optional.empty();
                    }
                    return Optional.of(toStateDto(match, profile, null));
                });
    }

    @Transactional(readOnly = true)
    public FriendMatchStateDto getStateForUser(String matchId, String userId) {
        ActiveMatchModel match = requireMatchById(matchId);
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        return toStateDto(match, profile, null);
    }

    @Transactional
    public void armTurnDeadline(String matchId) {
        ActiveMatchModel match = requireMatchById(matchId);
        if (match.getStatus() != MatchStatus.ACTIVE || match.getCurrentTurn() == null) {
            return;
        }
        if (!match.isHumanTurn(match.getCurrentTurn())) {
            match.setTurnDeadlineAt(null);
            saveMatch(match);
            return;
        }
        match.setTurnSequence(match.getTurnSequence() + 1);
        match.setTurnDeadlineAt(LocalDateTime.now().plusSeconds(GameConstants.FRIEND_TURN_TIMEOUT_SECONDS));
        saveMatch(match);
    }

    @Transactional(readOnly = true)
    public long currentTurnSequence(String matchId) {
        return requireMatchById(matchId).getTurnSequence();
    }

    @Transactional(readOnly = true)
    public LocalDateTime turnDeadline(String matchId) {
        return requireMatchById(matchId).getTurnDeadlineAt();
    }

    @Transactional
    public TurnTimeoutStep processTurnTimeout(String matchId, long expectedSequence) {
        return friendMatchStore.callExclusive(matchId, () -> processTurnTimeoutLocked(matchId, expectedSequence));
    }

    private TurnTimeoutStep processTurnTimeoutLocked(String matchId, long expectedSequence) {
        ActiveMatchModel match = requireMatchById(matchId);
        if (match.getStatus() != MatchStatus.ACTIVE) {
            return null;
        }
        if (match.getTurnSequence() != expectedSequence) {
            return null;
        }
        MatchPlayerSide side = match.getCurrentTurn();
        if (side == null || match.isSideControlledByBot(side)) {
            return null;
        }

        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        List<PokemonModel> allPokemon = pokemonRepository.findAllByOrderByPokedexNumberAsc();
        PokemonModel autoGuess = pickRandomUnusedGuess(match, side, allPokemon);
        if (autoGuess == null) {
            return null;
        }

        MatchEngine.ApplyGuessResult result = applyGuessSafe(match, side, autoGuess, pokemonByDex);
        result.guess().setTimedOut(true);
        result.guess().setAutoSelected(true);
        saveMatch(match);

        GameHistoryEntryDto history = finalizeIfFinished(match, null);
        if (match.getStatus() == MatchStatus.FINISHED) {
            completeIfFinished(match, null, null);
        }
        FriendMatchStateDto hostView = toStateDto(match, match.getProfile(), history, null);
        FriendMatchStateDto guestView = toStateDto(match, match.getGuestProfile(), history, null);
        BotMatchGuessFeedbackDto feedback = toFeedback(result, pokemonByDex);
        return new TurnTimeoutStep(hostView, guestView, feedback);
    }

    @Transactional
    public FriendMatchActionResponse surrender(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        String matchId = requireActiveFriendMatch(profile).getId();
        return friendMatchStore.callExclusive(matchId, () -> surrenderLocked(userId, profile, matchId));
    }

    private FriendMatchActionResponse surrenderLocked(String userId, ProfileModel profile, String matchId) {
        ActiveMatchModel match = requireMatchById(matchId);

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }

        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }
        ensureGuestJoined(match);

        MatchPlayerSide side = resolveSide(match, profile);
        MatchEngine.finishBySurrender(match, side);
        saveMatch(match);

        GameHistoryEntryDto history = finalizeIfFinished(match, side);
        String hostUserId = match.getProfile().getUser().getIdUser();
        String guestUserId = match.getGuestProfile().getUser().getIdUser();
        FriendMatchStateDto hostView = toStateDto(match, match.getProfile(), history, side);
        FriendMatchStateDto guestView = toStateDto(match, match.getGuestProfile(), history, side);
        FriendMatchStateDto surrendererView = toStateDto(match, profile, history, side);

        MatchRewardDto reward = completeIfFinished(match, side, userId);
        friendMatchTurnCoordinator.afterSurrender(matchId);

        return new FriendMatchActionResponse(surrendererView, List.of(), reward);
    }

    @Transactional
    public void abandonSetupMatch(String userId) {
        leaveMatch(userId);
    }

    /**
     * Sai da partida amigo em memória (sala, jogo ativo ou resíduo terminado) e remove
     * registos bot/local órfãos na BD que bloqueiam uma nova partida.
     */
    @Transactional
    public void leaveMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        Optional<ActiveMatchModel> memoryMatch = friendMatchStore.findActiveForProfile(profile.getId());

        if (memoryMatch.isPresent()) {
            ActiveMatchModel match = memoryMatch.get();
            if (match.getStatus() == MatchStatus.SETUP) {
                friendMatchStore.remove(match.getId());
                return;
            }
            if (match.getStatus() == MatchStatus.ACTIVE) {
                surrender(userId);
                return;
            }
            if (match.getStatus() == MatchStatus.FINISHED) {
                friendMatchStore.remove(match.getId());
                return;
            }
        }

        activeMatchConstraintService.clearStaleClientSideMatches(profile.getId());
    }

    private GameHistoryEntryDto finalizeIfFinished(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        if (match.getStatus() != MatchStatus.FINISHED) {
            return null;
        }
        return gameHistoryService.saveFriendGameFromActiveMatch(match, surrenderSide);
    }

    private MatchRewardDto completeIfFinished(
            ActiveMatchModel match,
            MatchPlayerSide surrenderSide,
            String rewardForUserId
    ) {
        if (match.getStatus() != MatchStatus.FINISHED) {
            return null;
        }
        return matchRewardService.grantAndRemoveActiveMatch(match, surrenderSide, rewardForUserId);
    }

    private FriendMatchStateDto toStateDto(
            ActiveMatchModel match,
            ProfileModel viewerProfile,
            GameHistoryEntryDto history
    ) {
        return toStateDto(match, viewerProfile, history, null);
    }

    private FriendMatchStateDto toStateDto(
            ActiveMatchModel match,
            ProfileModel viewerProfile,
            GameHistoryEntryDto history,
            MatchPlayerSide surrenderSide
    ) {
        MatchPlayerSide viewerSide = resolveSide(match, viewerProfile);
        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        List<OpponentSlotKnowledgeDto> knowledge = matchKnowledgeService.buildKnowledge(match, viewerSide);

        List<BotMatchGuessFeedbackDto> recentGuesses = List.copyOf(match.getGuesses()).stream()
                .limit(20)
                .map(g -> {
                    PokemonModel p = pokemonByDex.get(g.getGuessedPokedexNumber());
                    String name = p != null ? p.getName() : String.valueOf(g.getGuessedPokedexNumber());
                    return BotMatchGuessFeedbackDto.from(g, name, null, null);
                })
                .toList();

        MatchRewardDto yourReward = history != null && match.getStatus() == MatchStatus.FINISHED
                ? matchRewardService.previewRewardForProfile(match, viewerProfile, surrenderSide)
                : null;

        return FriendMatchStateDto.from(match, viewerSide, knowledge, recentGuesses, history, yourReward);
    }

    private ActiveMatchModel requireActiveFriendMatch(ProfileModel profile) {
        return friendMatchStore.findActiveForProfile(profile.getId())
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.GAME_FRIEND_MATCH_NOT_FOUND,
                        MessageKeys.GAME_FRIEND_MATCH_NOT_FOUND
                ));
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
            return MatchPlayerSide.HOST;
        }
        if (match.getGuestProfile() != null && match.getGuestProfile().getId().equals(profile.getId())) {
            return MatchPlayerSide.OPPONENT;
        }
        throw new ApiBusinessException(
                HttpStatus.FORBIDDEN,
                ErrorCodes.GAME_MATCH_NOT_FOUND,
                MessageKeys.GAME_FRIEND_MATCH_NOT_FOUND
        );
    }

    private static ActiveMatchPlayerModel getPlayer(ActiveMatchModel match, MatchPlayerSide side) {
        return side == MatchPlayerSide.HOST ? match.getHostPlayer() : match.getOpponentPlayer();
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

    private PokemonModel requirePokemon(int pokedexNumber) {
        return pokemonRepository.findByPokedexNumber(pokedexNumber)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.POKEMON_SPECIES_NOT_FOUND,
                        MessageKeys.POKEMON_SPECIES_NOT_FOUND
                ));
    }

    private void ensureNotAlreadyGuessed(ActiveMatchModel match, MatchPlayerSide side, int pokedexNumber) {
        boolean already = List.copyOf(match.getGuesses()).stream()
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

    private ActiveMatchModel requireMatchById(String matchId) {
        return friendMatchStore.findById(matchId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.GAME_FRIEND_MATCH_NOT_FOUND,
                        MessageKeys.GAME_FRIEND_MATCH_NOT_FOUND
                ));
    }

    private ActiveMatchModel saveMatch(ActiveMatchModel match) {
        friendMatchStore.save(match);
        return match;
    }

    private static PokemonModel pickRandomUnusedGuess(
            ActiveMatchModel match,
            MatchPlayerSide side,
            List<PokemonModel> allPokemon
    ) {
        Set<Integer> used = match.getGuesses().stream()
                .filter(g -> g.getPlayerSide() == side)
                .map(ActiveMatchGuessModel::getGuessedPokedexNumber)
                .collect(Collectors.toSet());
        List<PokemonModel> available = allPokemon.stream()
                .filter(p -> !used.contains(p.getPokedexNumber()))
                .toList();
        if (available.isEmpty()) {
            return null;
        }
        return available.get(ThreadLocalRandom.current().nextInt(available.size()));
    }
}
