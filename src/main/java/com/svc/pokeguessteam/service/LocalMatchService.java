package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.LocalMatchActionResponse;
import com.svc.pokeguessteam.dto.game.LocalMatchStartRequest;
import com.svc.pokeguessteam.dto.game.LocalMatchStateDto;
import com.svc.pokeguessteam.dto.game.LocalMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.OpponentSlotKnowledgeDto;
import com.svc.pokeguessteam.dto.game.OpponentTeamKnowledgeResponse;
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
import com.svc.pokeguessteam.util.GameConstants;
import com.svc.pokeguessteam.util.GameFinishValidation;
import com.svc.pokeguessteam.util.MatchEngine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocalMatchService {

    private final ActiveMatchRepository activeMatchRepository;
    private final PokemonRepository pokemonRepository;
    private final ProfileService profileService;
    private final GameHistoryService gameHistoryService;
    private final MatchRewardService matchRewardService;
    private final MatchKnowledgeService matchKnowledgeService;
    private final ActiveMatchConstraintService activeMatchConstraintService;

    public LocalMatchService(
            ActiveMatchRepository activeMatchRepository,
            PokemonRepository pokemonRepository,
            ProfileService profileService,
            GameHistoryService gameHistoryService,
            MatchRewardService matchRewardService,
            MatchKnowledgeService matchKnowledgeService,
            ActiveMatchConstraintService activeMatchConstraintService
    ) {
        this.activeMatchRepository = activeMatchRepository;
        this.pokemonRepository = pokemonRepository;
        this.profileService = profileService;
        this.gameHistoryService = gameHistoryService;
        this.matchRewardService = matchRewardService;
        this.matchKnowledgeService = matchKnowledgeService;
        this.activeMatchConstraintService = activeMatchConstraintService;
    }

    @Transactional
    public LocalMatchStateDto startMatch(String userId, LocalMatchStartRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        String opponentName = GameFinishValidation.validateAndNormalizeLocalOpponentName(request.opponentName());

        activeMatchConstraintService.ensureCanStartNewMatch(profile.getId());

        ActiveMatchModel match = new ActiveMatchModel();
        match.setProfile(profile);
        match.setGameMode(GameModes.LOCAL);
        match.setStatus(MatchStatus.SETUP);
        match.setOpponentName(opponentName);

        ActiveMatchPlayerModel player = new ActiveMatchPlayerModel();
        player.setSide(MatchPlayerSide.HOST);
        player.setSkipTurns(0);
        match.setHostPlayer(player);

        ActiveMatchPlayerModel opponent = new ActiveMatchPlayerModel();
        opponent.setSide(MatchPlayerSide.OPPONENT);
        opponent.setSkipTurns(0);
        match.setOpponentPlayer(opponent);

        ActiveMatchModel saved = activeMatchRepository.save(match);
        return toStateDto(saved, null);
    }

    @Transactional(readOnly = true)
    public LocalMatchStateDto getActiveMatch(String userId) {
        ActiveMatchModel match = requireActiveMatch(userId);
        return toStateDto(match, null);
    }

    @Transactional(readOnly = true)
    public OpponentTeamKnowledgeResponse getOpponentKnowledge(String userId) {
        ActiveMatchModel match = requireActiveMatch(userId);
        return matchKnowledgeService.getOpponentKnowledgeForCurrentTurn(match);
    }

    @Transactional
    public LocalMatchActionResponse submitTeam(String userId, LocalMatchTeamRequest request) {
        ActiveMatchModel match = requireActiveMatch(userId);
        if (match.getStatus() != MatchStatus.SETUP) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_PHASE,
                    MessageKeys.GAME_MATCH_INVALID_PHASE
            );
        }

        MatchPlayerSide side = request.playerSide();
        if (side != MatchPlayerSide.HOST && side != MatchPlayerSide.OPPONENT) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_ACTION,
                    MessageKeys.GAME_MATCH_INVALID_ACTION
            );
        }

        List<Integer> team = validateTeam(request.team());
        getPlayer(match, side).setTeam(team);

        MatchEngine.tryStartIfBothTeamsReady(match, GameConstants.TEAM_SIZE);
        activeMatchRepository.save(match);

        return new LocalMatchActionResponse(toStateDto(match, null), List.of());
    }

    @Transactional
    public LocalMatchActionResponse submitGuess(String userId, BotMatchGuessRequest request) {
        ActiveMatchModel match = requireActiveMatch(userId);
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }

        MatchPlayerSide side = match.getCurrentTurn();
        PokemonModel guessed = requirePokemon(request.pokedexNumber());
        ensureNotAlreadyGuessed(match, side, guessed.getPokedexNumber());

        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        MatchEngine.ApplyGuessResult result = applyGuessSafe(match, side, guessed, pokemonByDex);

        activeMatchRepository.save(match);
        GameHistoryEntryDto history = finalizeIfFinished(match, null);

        List<BotMatchGuessFeedbackDto> feedbacks = List.of(toFeedback(result, pokemonByDex));
        LocalMatchStateDto state = toStateDto(match, history);
        completeIfFinished(match, null);
        return new LocalMatchActionResponse(state, feedbacks);
    }

    @Transactional
    public LocalMatchActionResponse surrender(String userId) {
        ActiveMatchModel match = requireActiveMatch(userId);
        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }

        MatchPlayerSide surrenderSide = match.getStatus() == MatchStatus.ACTIVE
                ? match.getCurrentTurn()
                : MatchPlayerSide.HOST;
        MatchEngine.finishBySurrender(match, surrenderSide);
        activeMatchRepository.save(match);
        GameHistoryEntryDto history = finalizeIfFinished(match, surrenderSide);

        LocalMatchStateDto state = toStateDto(match, history);
        completeIfFinished(match, surrenderSide);
        return new LocalMatchActionResponse(state, List.of());
    }

    private GameHistoryEntryDto finalizeIfFinished(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        if (match.getStatus() != MatchStatus.FINISHED) {
            return null;
        }
        return gameHistoryService.saveLocalGameFromActiveMatch(match, surrenderSide);
    }

    private void completeIfFinished(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        if (match.getStatus() != MatchStatus.FINISHED) {
            return;
        }
        matchRewardService.grantAndRemoveActiveMatch(match, surrenderSide);
    }

    private LocalMatchStateDto toStateDto(ActiveMatchModel match, GameHistoryEntryDto history) {
        MatchPlayerSide knowledgeSide = match.getCurrentTurn() != null
                ? match.getCurrentTurn()
                : MatchPlayerSide.HOST;

        List<OpponentSlotKnowledgeDto> knowledge = matchKnowledgeService.buildKnowledge(match, knowledgeSide);
        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();

        List<BotMatchGuessFeedbackDto> recentGuesses = match.getGuesses().stream()
                .limit(20)
                .map(g -> {
                    PokemonModel p = pokemonByDex.get(g.getGuessedPokedexNumber());
                    String name = p != null ? p.getName() : String.valueOf(g.getGuessedPokedexNumber());
                    return BotMatchGuessFeedbackDto.from(g, name, null, null);
                })
                .toList();

        return LocalMatchStateDto.from(match, knowledge, recentGuesses, history);
    }

    private ActiveMatchModel requireActiveMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        return activeMatchRepository.findActiveByProfileIdAndGameMode(
                profile.getId(),
                GameModes.LOCAL,
                MatchStatus.FINISHED
        ).orElseThrow(() -> new ApiBusinessException(
                HttpStatus.NOT_FOUND,
                ErrorCodes.GAME_LOCAL_MATCH_NOT_FOUND,
                MessageKeys.GAME_LOCAL_MATCH_NOT_FOUND
        ));
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
}
