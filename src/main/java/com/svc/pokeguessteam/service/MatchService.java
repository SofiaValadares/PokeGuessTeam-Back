package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.CreateMatchRequest;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.dto.GuessResultResponse;
import com.svc.pokeguessteam.dto.MatchStateResponse;
import com.svc.pokeguessteam.model.MatchGuessLogModel;
import com.svc.pokeguessteam.model.MatchModel;
import com.svc.pokeguessteam.model.MatchTeamSlotModel;
import com.svc.pokeguessteam.model.PokemonModel;
import com.svc.pokeguessteam.model.enums.MatchMode;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.repository.MatchGuessLogRepository;
import com.svc.pokeguessteam.repository.MatchRepository;
import com.svc.pokeguessteam.repository.MatchTeamSlotRepository;
import com.svc.pokeguessteam.repository.PokemonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private static final String BOT_USER_ID = "BOT";

    private final MatchRepository matchRepository;
    private final MatchTeamSlotRepository slotRepository;
    private final MatchGuessLogRepository guessLogRepository;
    private final PokemonRepository pokemonRepository;

    public MatchService(MatchRepository matchRepository,
                        MatchTeamSlotRepository slotRepository,
                        MatchGuessLogRepository guessLogRepository,
                        PokemonRepository pokemonRepository) {
        this.matchRepository = matchRepository;
        this.slotRepository = slotRepository;
        this.guessLogRepository = guessLogRepository;
        this.pokemonRepository = pokemonRepository;
    }

    @Transactional
    public MatchStateResponse createBotMatch(String userId, CreateMatchRequest request) {
        if (request.playerTeamPokemonIds().size() != 6) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_TEAM_SIZE,
                    MessageKeys.GAME_TEAM_SIZE
            );
        }

        List<PokemonModel> playerTeam = request.playerTeamPokemonIds().stream()
                .map(this::findPokemon)
                .toList();

        List<PokemonModel> botTeam = pokemonRepository.findAll();
        if (botTeam.size() < 6) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_POKEDEX_INSUFFICIENT,
                    MessageKeys.GAME_POKEDEX_INSUFFICIENT
            );
        }
        Collections.shuffle(botTeam);
        botTeam = botTeam.stream().limit(6).toList();

        MatchModel match = new MatchModel();
        match.setPlayerAUserId(userId);
        match.setPlayerBUserId(BOT_USER_ID);
        match.setMode(MatchMode.BOT);
        matchRepository.save(match);

        saveTeam(match, "A", playerTeam);
        saveTeam(match, "B", botTeam);

        return getState(match.getId(), userId);
    }

    @Transactional
    public GuessResultResponse makeGuess(String matchId, String userId, String guessedPokemonId) {
        MatchModel match = findMatch(matchId);
        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.GAME_MATCH_FINISHED,
                    MessageKeys.GAME_MATCH_FINISHED
            );
        }

        String playerSide = resolveSide(match, userId);
        if (!match.getCurrentTurnSide().equals(playerSide)) {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.GAME_NOT_YOUR_TURN,
                    MessageKeys.GAME_NOT_YOUR_TURN
            );
        }

        String opponentSide = playerSide.equals("A") ? "B" : "A";
        PokemonModel guessed = findPokemon(guessedPokemonId);
        List<MatchTeamSlotModel> opponentSlots = slotRepository
                .findByMatch_IdAndOwnerSideOrderBySlotIndexAsc(match.getId(), opponentSide);

        List<Integer> exact = new ArrayList<>();
        List<Integer> gen = new ArrayList<>();
        List<Integer> type = new ArrayList<>();
        List<Integer> color = new ArrayList<>();
        List<Integer> height = new ArrayList<>();
        List<Integer> weight = new ArrayList<>();

        for (MatchTeamSlotModel slot : opponentSlots) {
            PokemonModel target = slot.getPokemon();
            int index = slot.getSlotIndex();

            if (target.getId().equals(guessed.getId())) {
                exact.add(index);
                if (!Boolean.TRUE.equals(slot.getDiscoveredByOpponent())) {
                    slot.setDiscoveredByOpponent(true);
                    slotRepository.save(slot);
                }
            }
            if (target.getGeneration().equals(guessed.getGeneration())) {
                gen.add(index);
            }
            if (hasSharedType(target, guessed)) {
                type.add(index);
            }
            if (target.getColor().equalsIgnoreCase(guessed.getColor())) {
                color.add(index);
            }
            if (Math.abs(target.getHeightDm() - guessed.getHeightDm()) <= 3) {
                height.add(index);
            }
            if (Math.abs(target.getWeightHg() - guessed.getWeightHg()) <= 50) {
                weight.add(index);
            }
        }

        MatchGuessLogModel log = new MatchGuessLogModel();
        log.setMatch(match);
        log.setGuesserSide(playerSide);
        log.setGuessedPokemon(guessed);
        log.setHitSlotCount(exact.size());
        log.setHitSlotIndexes(join(exact));
        log.setMatchedGenerationSlots(join(gen));
        log.setMatchedTypeSlots(join(type));
        log.setMatchedColorSlots(join(color));
        log.setMatchedHeightSlots(join(height));
        log.setMatchedWeightSlots(join(weight));
        guessLogRepository.save(log);

        if (!exact.isEmpty()) {
            incrementScore(match, playerSide, exact.size());
        } else {
            match.setCurrentTurnSide(opponentSide);
        }

        checkFinish(match, playerSide, opponentSide);
        matchRepository.save(match);

        return new GuessResultResponse(
                match.getId(),
                guessed.getId(),
                guessed.getName(),
                exact,
                gen,
                type,
                color,
                height,
                weight,
                match.getCurrentTurnSide(),
                scoreForSide(match, playerSide),
                scoreForSide(match, opponentSide),
                match.getStatus().name(),
                match.getWinnerUserId()
        );
    }

    public MatchStateResponse getState(String matchId, String userId) {
        MatchModel match = findMatch(matchId);
        String playerSide = resolveSide(match, userId);
        String opponentSide = playerSide.equals("A") ? "B" : "A";

        List<MatchTeamSlotModel> playerSlots = slotRepository.findByMatch_IdAndOwnerSideOrderBySlotIndexAsc(matchId, playerSide);
        List<MatchTeamSlotModel> opponentSlots = slotRepository.findByMatch_IdAndOwnerSideOrderBySlotIndexAsc(matchId, opponentSide);
        List<MatchGuessLogModel> logs = guessLogRepository.findByMatch_IdOrderByCreatedAtAsc(matchId);

        return new MatchStateResponse(
                match.getId(),
                match.getStatus().name(),
                match.getCurrentTurnSide(),
                scoreForSide(match, playerSide),
                scoreForSide(match, opponentSide),
                playerSlots.stream().map(slot -> new MatchStateResponse.MatchSlotResponse(
                        slot.getSlotIndex(),
                        slot.getPokemon().getId(),
                        slot.getPokemon().getName(),
                        true
                )).toList(),
                opponentSlots.stream().map(slot -> new MatchStateResponse.MatchSlotResponse(
                        slot.getSlotIndex(),
                        Boolean.TRUE.equals(slot.getDiscoveredByOpponent()) ? slot.getPokemon().getId() : null,
                        Boolean.TRUE.equals(slot.getDiscoveredByOpponent()) ? slot.getPokemon().getName() : "???",
                        slot.getDiscoveredByOpponent()
                )).toList(),
                logs.stream().map(log -> new MatchStateResponse.GuessLogResponse(
                        log.getGuesserSide(),
                        log.getGuessedPokemon().getName(),
                        log.getHitSlotCount(),
                        log.getHitSlotIndexes(),
                        log.getMatchedGenerationSlots(),
                        log.getMatchedTypeSlots(),
                        log.getMatchedColorSlots(),
                        log.getMatchedHeightSlots(),
                        log.getMatchedWeightSlots()
                )).collect(Collectors.toList())
        );
    }

    private MatchModel findMatch(String matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.GAME_MATCH_NOT_FOUND,
                        MessageKeys.GAME_MATCH_NOT_FOUND
                ));
    }

    private PokemonModel findPokemon(String id) {
        return pokemonRepository.findById(id)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.GAME_POKEMON_NOT_FOUND,
                        MessageKeys.GAME_POKEMON_NOT_FOUND,
                        id
                ));
    }

    private void saveTeam(MatchModel match, String side, List<PokemonModel> team) {
        int slot = 1;
        for (PokemonModel pokemon : team) {
            MatchTeamSlotModel model = new MatchTeamSlotModel();
            model.setMatch(match);
            model.setOwnerSide(side);
            model.setSlotIndex(slot++);
            model.setPokemon(pokemon);
            model.setDiscoveredByOpponent(false);
            slotRepository.save(model);
        }
    }

    private String resolveSide(MatchModel match, String userId) {
        if (match.getPlayerAUserId().equals(userId)) {
            return "A";
        }
        if (match.getPlayerBUserId().equals(userId)) {
            return "B";
        }
        throw new ApiBusinessException(
                HttpStatus.FORBIDDEN,
                ErrorCodes.GAME_USER_NOT_IN_MATCH,
                MessageKeys.GAME_USER_NOT_IN_MATCH
        );
    }

    private String join(List<Integer> values) {
        if (values.isEmpty()) {
            return "";
        }
        return values.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private void incrementScore(MatchModel match, String side, int amount) {
        if (side.equals("A")) {
            match.setPlayerAScore(match.getPlayerAScore() + amount);
        } else {
            match.setPlayerBScore(match.getPlayerBScore() + amount);
        }
    }

    private Integer scoreForSide(MatchModel match, String side) {
        return side.equals("A") ? match.getPlayerAScore() : match.getPlayerBScore();
    }

    private boolean hasSharedType(PokemonModel a, PokemonModel b) {
        if (a.getPrimaryType().equalsIgnoreCase(b.getPrimaryType())) {
            return true;
        }
        if (a.getPrimaryType().equalsIgnoreCase(safe(b.getSecondaryType()))) {
            return true;
        }
        if (safe(a.getSecondaryType()).equalsIgnoreCase(b.getPrimaryType())) {
            return true;
        }
        return safe(a.getSecondaryType()).equalsIgnoreCase(safe(b.getSecondaryType()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void checkFinish(MatchModel match, String currentSide, String opponentSide) {
        long discoveredOpponent = slotRepository.countByMatch_IdAndOwnerSideAndDiscoveredByOpponentTrue(match.getId(), opponentSide);
        if (discoveredOpponent < 6) {
            return;
        }
        if (!Boolean.TRUE.equals(match.getForceDrawActive())) {
            match.setForceDrawActive(true);
            match.setForceDrawSide(opponentSide);
            match.setCurrentTurnSide(opponentSide);
            return;
        }
        if (opponentSide.equals(match.getForceDrawSide())) {
            if (scoreForSide(match, opponentSide).equals(scoreForSide(match, currentSide))) {
                match.setStatus(MatchStatus.FINISHED);
                match.setWinnerUserId("DRAW");
                return;
            }
            match.setStatus(MatchStatus.FINISHED);
            match.setWinnerUserId(currentSide.equals("A") ? match.getPlayerAUserId() : match.getPlayerBUserId());
        }
    }
}
