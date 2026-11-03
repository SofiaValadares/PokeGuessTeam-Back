package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.BotMatchGuessRequest;
import com.svc.pokeguessteam.dto.game.BotMatchSetupResponse;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchActionResponse;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.dto.game.GameBotFinishRequest;
import com.svc.pokeguessteam.dto.game.GameFinishResponse;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.MatchRewardDto;
import com.svc.pokeguessteam.dto.game.OpponentSlotKnowledgeDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.util.BotMatchFinishResolver;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class BotMatchService {

    private final ActiveMatchRepository activeMatchRepository;
    private final ProfileService profileService;
    private final GameHistoryService gameHistoryService;
    private final MatchRewardService matchRewardService;
    private final ActiveMatchConstraintService activeMatchConstraintService;
    private final DuelTeamService duelTeamService;
    private final ActiveMatchRemovalService activeMatchRemovalService;
    private final ClientMatchCommitmentService clientMatchCommitmentService;
    private final ActiveMatchTeamService activeMatchTeamService;
    private final MatchKnowledgeService matchKnowledgeService;
    private final NationalPokedexCatalog nationalPokedexCatalog;
    private final TeamCommitmentService teamCommitmentService;

    public BotMatchService(
            ActiveMatchRepository activeMatchRepository,
            ProfileService profileService,
            GameHistoryService gameHistoryService,
            MatchRewardService matchRewardService,
            ActiveMatchConstraintService activeMatchConstraintService,
            DuelTeamService duelTeamService,
            ActiveMatchRemovalService activeMatchRemovalService,
            ClientMatchCommitmentService clientMatchCommitmentService,
            ActiveMatchTeamService activeMatchTeamService,
            MatchKnowledgeService matchKnowledgeService,
            NationalPokedexCatalog nationalPokedexCatalog,
            TeamCommitmentService teamCommitmentService
    ) {
        this.activeMatchRepository = activeMatchRepository;
        this.profileService = profileService;
        this.gameHistoryService = gameHistoryService;
        this.matchRewardService = matchRewardService;
        this.activeMatchConstraintService = activeMatchConstraintService;
        this.duelTeamService = duelTeamService;
        this.activeMatchRemovalService = activeMatchRemovalService;
        this.clientMatchCommitmentService = clientMatchCommitmentService;
        this.activeMatchTeamService = activeMatchTeamService;
        this.matchKnowledgeService = matchKnowledgeService;
        this.nationalPokedexCatalog = nationalPokedexCatalog;
        this.teamCommitmentService = teamCommitmentService;
    }

    /** Valida a equipa na Pokédex do user, persiste times para /guess; crypto só no finish. */
    @Transactional
    public BotMatchSetupResponse validateTeamForClient(String userId, BotMatchTeamRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        clearStaleBotMatches(profile.getId());
        activeMatchConstraintService.ensureCanStartNewMatch(profile.getId());

        Set<Integer> registered = duelTeamService.registeredNumbers(userId);
        List<Integer> team = duelTeamService.validateTeam(registered, request.team(), null);
        int minForBotDuel = GameConstants.TEAM_SIZE * 2;
        if (registered.size() < minForBotDuel) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_POKEDEX_INSUFFICIENT,
                    MessageKeys.GAME_POKEDEX_INSUFFICIENT,
                    minForBotDuel,
                    registered.size()
            );
        }
        Set<Integer> excluded = new HashSet<>(team);
        List<Integer> botTeam = duelTeamService.buildBotTeamFromRegistered(registered, excluded);
        ClientMatchCommitmentService.CommittedClientMatch committed =
                clientMatchCommitmentService.commit(profile, GameModes.BOT, "PokéBot", team, botTeam);
        ActiveMatchModel match = activeMatchRepository.findById(committed.matchId())
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.GAME_MATCH_NOT_FOUND,
                        MessageKeys.GAME_MATCH_NOT_FOUND
                ));
        return new BotMatchSetupResponse(
                committed.matchId(),
                committed.host().team(),
                committed.host().commitment(),
                committed.opponent().commitment(),
                match.getStartingPlayer(),
                match.getCurrentTurn()
        );
    }

    /**
     * Mesmo fluxo do guess online: MatchEngine + FriendMatchActionResponse
     * (estado com opponentKnowledge). Após o palpite do host, o bot joga até
     * voltar a vez do host ou a partida terminar.
     */
    @Transactional
    public FriendMatchActionResponse submitGuess(String userId, BotMatchGuessRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveBotMatch(profile.getId(), request.matchId());
        hydrateMatch(match);

        if (match.getCurrentTurn() != MatchPlayerSide.HOST) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_WRONG_TURN,
                    MessageKeys.GAME_MATCH_WRONG_TURN
            );
        }

        PokemonModel guessed = requirePokemon(request.pokedexNumber());
        Set<Integer> registered = duelTeamService.registeredNumbers(userId);
        if (!registered.contains(guessed.getPokedexNumber())) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_GUESS_NOT_IN_POKEDEX,
                    MessageKeys.GAME_GUESS_NOT_IN_POKEDEX
            );
        }
        ensureNotAlreadyGuessed(match, MatchPlayerSide.HOST, guessed.getPokedexNumber());

        Map<Integer, PokemonModel> pokemonByDex = loadPokemonByDex();
        List<BotMatchGuessFeedbackDto> feedbacks = new ArrayList<>();

        MatchEngine.ApplyGuessResult hostResult = applyGuessSafe(match, MatchPlayerSide.HOST, guessed, pokemonByDex);
        feedbacks.add(toFeedback(hostResult, pokemonByDex));

        List<PokemonModel> botPool = duelTeamService.modelsFromRegistered(registered);
        while (match.getStatus() == MatchStatus.ACTIVE && match.getCurrentTurn() == MatchPlayerSide.OPPONENT) {
            PokemonModel botGuess = pickUnusedGuess(match, MatchPlayerSide.OPPONENT, botPool);
            if (botGuess == null) {
                botGuess = pickUnusedGuess(match, MatchPlayerSide.OPPONENT, pokemonByDex.values().stream().toList());
            }
            if (botGuess == null) {
                throw new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.GAME_MATCH_INVALID_ACTION,
                        MessageKeys.GAME_MATCH_INVALID_ACTION
                );
            }
            MatchEngine.ApplyGuessResult botResult =
                    applyGuessSafe(match, MatchPlayerSide.OPPONENT, botGuess, pokemonByDex);
            botResult.guess().setAutoSelected(true);
            feedbacks.add(toFeedback(botResult, pokemonByDex));
        }

        activeMatchRepository.save(match);
        FriendMatchStateDto state = toStateDto(match, pokemonByDex);
        return new FriendMatchActionResponse(state, List.copyOf(feedbacks));
    }

    /** Abre os commitments (AES + SHA-256 + HMAC), persiste histórico e recompensas. */
    @Transactional
    public GameFinishResponse finishClientMatch(String userId, GameBotFinishRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        duelTeamService.validateTeamFromRegisteredPokedex(userId, request.hostTeam());
        ActiveMatchModel match = activeMatchRepository.findByIdAndProfile_Id(request.matchId(), profile.getId())
                .filter(stored -> stored.getGameMode() == GameModes.BOT)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.GAME_MATCH_NOT_FOUND,
                        MessageKeys.GAME_MATCH_NOT_FOUND
                ));
        hydrateMatch(match);
        GameResults result = BotMatchFinishResolver.resolve(match.getStatus(), match.getWinner(), request.result());
        GameBotFinishRequest trusted = new GameBotFinishRequest(
                request.matchId(),
                request.hostTeam(),
                match.getHostPlayer().getHits().size(),
                match.getOpponentPlayer().getHits().size(),
                result
        );
        ClientMatchCommitmentService.OpenedClientMatch opened = clientMatchCommitmentService.verifyHostAndOpenSealedOpponent(
                profile.getId(),
                GameModes.BOT,
                request.matchId(),
                request.hostTeam()
        );
        List<Integer> opponentTeam = opened.opponentOpening().team();
        GameHistoryEntryDto history = gameHistoryService.saveBotFinish(
                userId,
                trusted,
                request.hostTeam(),
                opponentTeam
        );
        MatchRewardDto reward = matchRewardService.grantForUser(userId, GameModes.BOT, result);
        return new GameFinishResponse(
                history,
                reward,
                opened.hostCommitment(),
                opened.opponentCommitment(),
                opened.hostOpening(),
                opened.opponentOpening()
        );
    }

    private FriendMatchStateDto toStateDto(ActiveMatchModel match, Map<Integer, PokemonModel> pokemonByDex) {
        MatchPlayerSide viewerSide = MatchPlayerSide.HOST;
        List<OpponentSlotKnowledgeDto> knowledge = matchKnowledgeService.buildKnowledge(match, viewerSide, pokemonByDex);
        List<BotMatchGuessFeedbackDto> recentGuesses = List.copyOf(match.getGuesses()).stream()
                .map(g -> {
                    PokemonModel p = pokemonByDex.get(g.getGuessedPokedexNumber());
                    String name = p != null ? p.getName() : String.valueOf(g.getGuessedPokedexNumber());
                    return BotMatchGuessFeedbackDto.from(g, name, null, null);
                })
                .toList();

        boolean opened = match.getStatus() == MatchStatus.FINISHED;
        return FriendMatchStateDto.from(
                match,
                viewerSide,
                knowledge,
                recentGuesses,
                null,
                null,
                opened ? teamCommitmentService.nonceOf(match.getHostPlayer()) : null,
                opened ? teamCommitmentService.nonceOf(match.getOpponentPlayer()) : null
        );
    }

    private void hydrateMatch(ActiveMatchModel match) {
        ActiveMatchPlayerModel host = match.getHostPlayer();
        ActiveMatchPlayerModel opponent = match.getOpponentPlayer();
        host.setTeam(activeMatchTeamService.loadTeam(host.getId()));
        opponent.setTeam(activeMatchTeamService.loadTeam(opponent.getId()));

        Set<Integer> hostHits = new HashSet<>();
        Set<Integer> opponentHits = new HashSet<>();
        for (ActiveMatchGuessModel guess : match.getGuesses()) {
            if (!guess.isExactMatch()) {
                continue;
            }
            if (guess.getPlayerSide() == MatchPlayerSide.HOST) {
                hostHits.addAll(guess.getMatchedPokedexNumbers());
            } else if (guess.getPlayerSide() == MatchPlayerSide.OPPONENT) {
                opponentHits.addAll(guess.getMatchedPokedexNumbers());
            }
        }
        host.loadHits(hostHits);
        opponent.loadHits(opponentHits);
    }

    private ActiveMatchModel requireActiveBotMatch(String profileId, String matchId) {
        ActiveMatchModel match = activeMatchRepository.findByIdAndProfile_Id(matchId, profileId)
                .filter(stored -> stored.getGameMode() == GameModes.BOT)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.GAME_MATCH_NOT_FOUND,
                        MessageKeys.GAME_MATCH_NOT_FOUND
                ));
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_MATCH_NOT_ACTIVE,
                    MessageKeys.GAME_MATCH_NOT_ACTIVE
            );
        }
        return match;
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
        PokemonModel pokemon = nationalPokedexCatalog.speciesByDex().get(pokedexNumber);
        if (pokemon == null) {
            throw new ApiBusinessException(
                    HttpStatus.NOT_FOUND,
                    ErrorCodes.POKEMON_SPECIES_NOT_FOUND,
                    MessageKeys.POKEMON_SPECIES_NOT_FOUND
            );
        }
        return pokemon;
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

    private static PokemonModel pickUnusedGuess(
            ActiveMatchModel match,
            MatchPlayerSide side,
            List<PokemonModel> pool
    ) {
        Set<Integer> used = match.getGuesses().stream()
                .filter(g -> g.getPlayerSide() == side)
                .map(ActiveMatchGuessModel::getGuessedPokedexNumber)
                .collect(Collectors.toSet());
        List<PokemonModel> available = pool.stream()
                .filter(p -> !used.contains(p.getPokedexNumber()))
                .toList();
        if (available.isEmpty()) {
            return null;
        }
        return available.get(ThreadLocalRandom.current().nextInt(available.size()));
    }

    private Map<Integer, PokemonModel> loadPokemonByDex() {
        return nationalPokedexCatalog.speciesByDex();
    }

    private void clearStaleBotMatches(String profileId) {
        activeMatchRepository.findAllActiveByProfileIdAndGameModeOrderByCreatedAtDesc(
                profileId,
                GameModes.BOT,
                MatchStatus.FINISHED
        ).forEach(match -> activeMatchRemovalService.deleteByMatchId(match.getId()));
    }
}
