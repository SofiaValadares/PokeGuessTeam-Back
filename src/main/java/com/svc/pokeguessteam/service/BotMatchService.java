package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchActionResponse;
import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.BotMatchStateDto;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.OpponentKnowledgeSlotDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GuessOutcome;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.util.BotAiOpponent;
import com.svc.pokeguessteam.util.GameConstants;
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
public class BotMatchService {

    private final ActiveMatchRepository activeMatchRepository;
    private final PokemonRepository pokemonRepository;
    private final ProfileService profileService;
    private final GameHistoryService gameHistoryService;

    public BotMatchService(
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
    public BotMatchStateDto startMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        activeMatchRepository.findActiveByProfileIdAndGameMode(
                profile.getId(),
                GameModes.BOT,
                MatchStatus.FINISHED
        ).ifPresent(existing -> activeMatchRepository.delete(existing));

        ActiveMatchModel match = new ActiveMatchModel();
        match.setProfile(profile);
        match.setGameMode(GameModes.BOT);
        match.setStatus(MatchStatus.SETUP);

        ActiveMatchPlayerModel userPlayer = new ActiveMatchPlayerModel();
        userPlayer.setSide(MatchPlayerSide.USER);
        userPlayer.setSkipTurns(0);
        match.setUserPlayer(userPlayer);

        ActiveMatchPlayerModel botPlayer = new ActiveMatchPlayerModel();
        botPlayer.setSide(MatchPlayerSide.BOT);
        botPlayer.setSkipTurns(0);
        match.setBotPlayer(botPlayer);

        ActiveMatchModel saved = activeMatchRepository.save(match);
        return toStateDto(saved, null);
    }

    @Transactional(readOnly = true)
    public BotMatchStateDto getActiveMatch(String userId) {
        ActiveMatchModel match = requireActiveMatch(userId);
        return toStateDto(match, null);
    }

    @Transactional
    public BotMatchActionResponse submitTeam(String userId, BotMatchTeamRequest request) {
        ActiveMatchModel match = requireActiveMatch(userId);
        if (match.getStatus() != MatchStatus.SETUP) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_INVALID_PHASE,
                    MessageKeys.GAME_MATCH_INVALID_PHASE
            );
        }

        List<Integer> team = validateTeam(request.team());
        match.getUserPlayer().setTeam(team);

        List<PokemonModel> allPokemon = pokemonRepository.findAllByOrderByPokedexNumberAsc();
        Set<Integer> excluded = new HashSet<>(team);
        List<Integer> botTeam = BotAiOpponent.buildRandomTeam(allPokemon, excluded, GameConstants.TEAM_SIZE);
        match.getBotPlayer().setTeam(botTeam);

        MatchEngine.startActiveMatch(match);
        activeMatchRepository.save(match);

        List<BotMatchGuessFeedbackDto> botFeedbacks = runBotTurnsIfNeeded(match, allPokemon);
        GameHistoryEntryDto history = finalizeIfFinished(match, false);

        return new BotMatchActionResponse(toStateDto(match, history), botFeedbacks);
    }

    @Transactional
    public BotMatchActionResponse submitGuess(String userId, BotMatchGuessRequest request) {
        ActiveMatchModel match = requireActiveMatch(userId);
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }
        if (match.getCurrentTurn() != MatchPlayerSide.USER) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_WRONG_TURN,
                    MessageKeys.GAME_MATCH_WRONG_TURN
            );
        }

        PokemonModel guessed = requirePokemon(request.pokedexNumber());
        ensureNotAlreadyGuessed(match, MatchPlayerSide.USER, guessed.getPokedexNumber());

        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        List<BotMatchGuessFeedbackDto> feedbacks = new ArrayList<>();

        MatchEngine.ApplyGuessResult userResult = applyGuessSafe(match, MatchPlayerSide.USER, guessed, pokemonByDex);
        feedbacks.add(toFeedback(userResult, pokemonByDex));

        List<PokemonModel> allPokemon = pokemonRepository.findAllByOrderByPokedexNumberAsc();
        feedbacks.addAll(runBotTurnsIfNeeded(match, allPokemon));

        activeMatchRepository.save(match);
        GameHistoryEntryDto history = finalizeIfFinished(match, false);

        return new BotMatchActionResponse(toStateDto(match, history), feedbacks);
    }

    @Transactional
    public BotMatchActionResponse surrender(String userId) {
        ActiveMatchModel match = requireActiveMatch(userId);
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }

        MatchEngine.finishBySurrender(match, MatchPlayerSide.USER);
        activeMatchRepository.save(match);
        GameHistoryEntryDto history = finalizeIfFinished(match, true);

        return new BotMatchActionResponse(toStateDto(match, history), List.of());
    }

    @Transactional
    public void abandonMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        activeMatchRepository.findActiveByProfileIdAndGameMode(
                profile.getId(),
                GameModes.BOT,
                MatchStatus.FINISHED
        ).ifPresent(activeMatchRepository::delete);
    }

    private GameHistoryEntryDto finalizeIfFinished(ActiveMatchModel match, boolean userSurrendered) {
        if (match.getStatus() != MatchStatus.FINISHED) {
            return null;
        }
        return gameHistoryService.saveBotGameFromActiveMatch(match, userSurrendered);
    }

    private List<BotMatchGuessFeedbackDto> runBotTurnsIfNeeded(
            ActiveMatchModel match,
            List<PokemonModel> allPokemon
    ) {
        Map<Integer, PokemonModel> pokemonByDex = allPokemon.stream()
                .collect(Collectors.toMap(PokemonModel::getPokedexNumber, Function.identity(), (a, b) -> a));

        List<BotMatchGuessFeedbackDto> feedbacks = new ArrayList<>();
        int safety = 50;
        while (match.getStatus() == MatchStatus.ACTIVE
                && match.getCurrentTurn() == MatchPlayerSide.BOT
                && safety-- > 0) {
            PokemonModel botGuess = BotAiOpponent.chooseGuess(allPokemon, match, MatchPlayerSide.BOT, pokemonByDex);
            if (botGuess == null) {
                break;
            }
            MatchEngine.ApplyGuessResult botResult = applyGuessSafe(
                    match,
                    MatchPlayerSide.BOT,
                    botGuess,
                    pokemonByDex
            );
            feedbacks.add(toFeedback(botResult, pokemonByDex));
        }
        return feedbacks;
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
            throw new ApiBusinessException(HttpStatus.BAD_REQUEST, ErrorCodes.GAME_MATCH_INVALID_ACTION, ex.getMessage());
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

    private BotMatchStateDto toStateDto(ActiveMatchModel match, GameHistoryEntryDto history) {
        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        List<OpponentKnowledgeSlotDto> knowledge = BotAiOpponent
                .buildOpponentKnowledge(match, MatchPlayerSide.USER, pokemonByDex)
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

        return BotMatchStateDto.from(match, knowledge, recentGuesses, history);
    }

    private ActiveMatchModel requireActiveMatch(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        return activeMatchRepository.findActiveByProfileIdAndGameMode(
                profile.getId(),
                GameModes.BOT,
                MatchStatus.FINISHED
        ).orElseThrow(() -> new ApiBusinessException(
                HttpStatus.NOT_FOUND,
                ErrorCodes.GAME_MATCH_NOT_FOUND,
                MessageKeys.GAME_MATCH_NOT_FOUND
        ));
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
