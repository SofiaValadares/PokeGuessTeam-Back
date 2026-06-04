package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchActionResponse;
import com.svc.pokeguessteam.dto.game.FriendMatchJoinRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
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
import com.svc.pokeguessteam.util.BotAiOpponent;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
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
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FriendMatchService {

    private final ActiveMatchRepository activeMatchRepository;
    private final PokemonRepository pokemonRepository;
    private final ProfileService profileService;
    private final GameHistoryService gameHistoryService;
    private final MatchRewardService matchRewardService;
    private final MatchKnowledgeService matchKnowledgeService;
    private final ActiveMatchConstraintService activeMatchConstraintService;
    private final FriendOnlineModerationService friendOnlineModerationService;
    private final FriendMatchRealtimeCoordinator friendMatchRealtimeCoordinator;

    public FriendMatchService(
            ActiveMatchRepository activeMatchRepository,
            PokemonRepository pokemonRepository,
            ProfileService profileService,
            GameHistoryService gameHistoryService,
            MatchRewardService matchRewardService,
            MatchKnowledgeService matchKnowledgeService,
            ActiveMatchConstraintService activeMatchConstraintService,
            FriendOnlineModerationService friendOnlineModerationService,
            @Lazy FriendMatchRealtimeCoordinator friendMatchRealtimeCoordinator
    ) {
        this.activeMatchRepository = activeMatchRepository;
        this.pokemonRepository = pokemonRepository;
        this.profileService = profileService;
        this.gameHistoryService = gameHistoryService;
        this.matchRewardService = matchRewardService;
        this.matchKnowledgeService = matchKnowledgeService;
        this.activeMatchConstraintService = activeMatchConstraintService;
        this.friendOnlineModerationService = friendOnlineModerationService;
        this.friendMatchRealtimeCoordinator = friendMatchRealtimeCoordinator;
    }

    public record BotReplacementStep(
            FriendMatchStateDto hostView,
            FriendMatchStateDto guestView,
            BotMatchGuessFeedbackDto feedback
    ) {
    }

    @Transactional
    public FriendMatchStateDto startMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        friendOnlineModerationService.ensureNotBanned(profile);
        activeMatchConstraintService.ensureCanStartNewMatch(profile.getId());

        ActiveMatchModel match = new ActiveMatchModel();
        match.setProfile(profile);
        match.setGameMode(GameModes.FRIEND);
        match.setStatus(MatchStatus.SETUP);
        match.setJoinCode(JoinCodeGenerator.generateUnique(activeMatchRepository));

        ActiveMatchPlayerModel hostPlayer = new ActiveMatchPlayerModel();
        hostPlayer.setSide(MatchPlayerSide.HOST);
        hostPlayer.setSkipTurns(0);
        match.setHostPlayer(hostPlayer);

        ActiveMatchPlayerModel guestPlayer = new ActiveMatchPlayerModel();
        guestPlayer.setSide(MatchPlayerSide.OPPONENT);
        guestPlayer.setSkipTurns(0);
        match.setOpponentPlayer(guestPlayer);

        ActiveMatchModel saved = activeMatchRepository.save(match);
        return toStateDto(saved, profile, null);
    }

    @Transactional
    public FriendMatchStateDto joinMatch(String userId, FriendMatchJoinRequest request) {
        ProfileModel guestProfile = profileService.ensureProfileWithStarters(userId);
        friendOnlineModerationService.ensureNotBanned(guestProfile);
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

        activeMatchConstraintService.ensureCanStartNewMatch(guestProfile.getId());
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

        boolean started = MatchEngine.tryStartIfBothTeamsReady(match, GameConstants.TEAM_SIZE);
        activeMatchRepository.save(match);

        FriendMatchStateDto viewerState = toStateDto(match, profile, null);
        if (started && match.getGuestProfile() != null) {
            String hostUserId = match.getProfile().getUser().getIdUser();
            String guestUserId = match.getGuestProfile().getUser().getIdUser();
            friendMatchRealtimeCoordinator.publishAfterTeamReady(
                    match.getId(),
                    hostUserId,
                    guestUserId,
                    getStateForUser(match.getId(), hostUserId),
                    getStateForUser(match.getId(), guestUserId)
            );
        }
        return new FriendMatchActionResponse(viewerState, List.of());
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
        FriendMatchStateDto state = toStateDto(match, profile, history);
        completeIfFinished(match, null);

        String hostUserId = match.getProfile().getUser().getIdUser();
        String guestUserId = match.getGuestProfile().getUser().getIdUser();
        friendMatchRealtimeCoordinator.publishAfterHumanGuess(
                new FriendMatchActionResponse(state, feedbacks),
                hostUserId,
                guestUserId
        );
        return new FriendMatchActionResponse(state, feedbacks);
    }

    @Transactional(readOnly = true)
    public FriendMatchStateDto getStateForUser(String matchId, String userId) {
        ActiveMatchModel match = activeMatchRepository.findDetailedById(matchId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.GAME_FRIEND_MATCH_NOT_FOUND,
                        MessageKeys.GAME_FRIEND_MATCH_NOT_FOUND
                ));
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
            activeMatchRepository.save(match);
            return;
        }
        match.setTurnSequence(match.getTurnSequence() + 1);
        match.setTurnDeadlineAt(LocalDateTime.now().plusSeconds(GameConstants.FRIEND_TURN_TIMEOUT_SECONDS));
        activeMatchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public long currentTurnSequence(String matchId) {
        return requireMatchById(matchId).getTurnSequence();
    }

    @Transactional(readOnly = true)
    public LocalDateTime turnDeadline(String matchId) {
        return requireMatchById(matchId).getTurnDeadlineAt();
    }

    @Transactional(readOnly = true)
    public boolean isBotControlledTurn(String matchId, MatchPlayerSide side) {
        ActiveMatchModel match = requireMatchById(matchId);
        return match.isSideControlledByBot(side);
    }

    @Transactional(readOnly = true)
    public boolean wasBotReplacementTriggered(String matchId) {
        return requireMatchById(matchId).getBotReplacementSide() != null;
    }

    @Transactional
    public FriendMatchActionResponse processTurnTimeout(String matchId, long expectedSequence) {
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

        ProfileModel penalizedProfile = side == MatchPlayerSide.HOST ? match.getProfile() : match.getGuestProfile();
        if (penalizedProfile == null) {
            return null;
        }

        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        PokemonModel autoGuess = pickRandomUnusedGuess(match, side, new ArrayList<>(pokemonRepository.findAllByOrderByPokedexNumberAsc()));
        if (autoGuess == null) {
            return null;
        }

        ActiveMatchPlayerModel player = getPlayer(match, side);
        player.setTurnTimeoutPenalties(player.getTurnTimeoutPenalties() + 1);
        friendOnlineModerationService.recordTimeoutPenalty(penalizedProfile, match);

        if (player.getTurnTimeoutPenalties() >= GameConstants.FRIEND_MAX_TIMEOUT_PENALTIES_PER_MATCH) {
            match.setBotReplacementSide(side);
        }

        MatchEngine.ApplyGuessResult result = applyGuessSafe(match, side, autoGuess, pokemonByDex);
        result.guess().setTimedOut(true);
        result.guess().setAutoSelected(true);
        activeMatchRepository.save(match);

        GameHistoryEntryDto history = finalizeIfFinished(match, match.getBotReplacementSide());
        ProfileModel viewerProfile = penalizedProfile;
        FriendMatchStateDto state = toStateDto(match, viewerProfile, history);
        completeIfFinished(match, match.getBotReplacementSide());
        return new FriendMatchActionResponse(state, List.of(toFeedback(result, pokemonByDex)));
    }

    @Transactional
    public BotReplacementStep processSingleBotReplacementTurn(String matchId) {
        ActiveMatchModel match = requireMatchById(matchId);
        if (match.getStatus() != MatchStatus.ACTIVE || match.getBotReplacementSide() == null) {
            return null;
        }
        MatchPlayerSide botSide = match.getBotReplacementSide();
        if (match.getCurrentTurn() != botSide) {
            return null;
        }

        List<PokemonModel> allPokemon = pokemonRepository.findAllByOrderByPokedexNumberAsc();
        Map<Integer, PokemonModel> pokemonByDex = allPokemon.stream()
                .collect(Collectors.toMap(PokemonModel::getPokedexNumber, Function.identity(), (a, b) -> a));
        PokemonModel botGuess = BotAiOpponent.chooseGuess(allPokemon, match, botSide, pokemonByDex);
        if (botGuess == null) {
            return null;
        }

        MatchEngine.ApplyGuessResult result = applyGuessSafe(match, botSide, botGuess, pokemonByDex);
        activeMatchRepository.save(match);
        GameHistoryEntryDto history = finalizeIfFinished(match, match.getBotReplacementSide());
        completeIfFinished(match, match.getBotReplacementSide());

        String hostUserId = match.getProfile().getUser().getIdUser();
        String guestUserId = match.getGuestProfile().getUser().getIdUser();
        return new BotReplacementStep(
                getStateForUser(matchId, hostUserId),
                getStateForUser(matchId, guestUserId),
                toFeedback(result, pokemonByDex)
        );
    }

    @Transactional
    public FriendMatchActionResponse surrender(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveFriendMatch(profile);

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }

        MatchPlayerSide side = resolveSide(match, profile);
        MatchEngine.finishBySurrender(match, side);
        activeMatchRepository.save(match);

        GameHistoryEntryDto history = null;
        if (match.getGuestProfile() != null) {
            history = finalizeIfFinished(match, side);
        }

        FriendMatchStateDto state = toStateDto(match, profile, history);
        completeIfFinished(match, side);
        return new FriendMatchActionResponse(state, List.of());
    }

    private GameHistoryEntryDto finalizeIfFinished(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        if (match.getStatus() != MatchStatus.FINISHED) {
            return null;
        }
        return gameHistoryService.saveFriendGameFromActiveMatch(match, surrenderSide);
    }

    private void completeIfFinished(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        if (match.getStatus() != MatchStatus.FINISHED) {
            return;
        }
        matchRewardService.grantAndRemoveActiveMatch(match, surrenderSide);
    }

    private FriendMatchStateDto toStateDto(
            ActiveMatchModel match,
            ProfileModel viewerProfile,
            GameHistoryEntryDto history
    ) {
        MatchPlayerSide viewerSide = resolveSide(match, viewerProfile);
        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        List<OpponentSlotKnowledgeDto> knowledge = matchKnowledgeService.buildKnowledge(match, viewerSide);

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

    private ActiveMatchModel requireMatchById(String matchId) {
        return activeMatchRepository.findDetailedById(matchId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.GAME_FRIEND_MATCH_NOT_FOUND,
                        MessageKeys.GAME_FRIEND_MATCH_NOT_FOUND
                ));
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
