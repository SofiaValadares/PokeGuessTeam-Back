package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.GameBotFinishRequest;
import com.svc.pokeguessteam.dto.game.GameFinishRequest;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.GameHistoryPageResponse;
import com.svc.pokeguessteam.dto.game.GameLocalFinishRequest;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.HistoryGameModel;
import com.svc.pokeguessteam.model.game.HistoryGamePlayerModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.HistoryGameRepository;
import com.svc.pokeguessteam.util.GameFinishValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameHistoryService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private static final int USER_SLOT = 1;
    private static final int OPPONENT_SLOT = 2;

    private final HistoryGameRepository historyGameRepository;
    private final ProfileService profileService;

    public GameHistoryService(
            HistoryGameRepository historyGameRepository,
            ProfileService profileService
    ) {
        this.historyGameRepository = historyGameRepository;
        this.profileService = profileService;
    }

    @Transactional
    public GameHistoryEntryDto saveBotFinish(String userId, GameBotFinishRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        validateFinishRequest(request);
        HistoryGameModel game = buildFinishedGame(
                GameModes.BOT,
                null,
                profile,
                null,
                request
        );
        return GameHistoryEntryDto.from(historyGameRepository.save(game));
    }

    @Transactional
    public GameHistoryEntryDto saveLocalFinish(String userId, GameLocalFinishRequest request) {
        String opponentName = GameFinishValidation.validateAndNormalizeLocalOpponentName(request.opponentName());
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        validateFinishRequest(request);
        HistoryGameModel game = buildFinishedGame(
                GameModes.LOCAL,
                opponentName,
                profile,
                null,
                request
        );
        return GameHistoryEntryDto.from(historyGameRepository.save(game));
    }

    /**
     * Persiste histórico a partir de partida ativa vs amigo (motor no servidor).
     */
    @Transactional
    public GameHistoryEntryDto saveFriendGameFromActiveMatch(
            ActiveMatchModel match,
            MatchPlayerSide surrenderSide
    ) {
        ProfileModel guest = match.getGuestProfile();
        if (guest == null) {
            throw new IllegalStateException("Partida amigo sem convidado.");
        }

        int hostHits = match.getHostPlayer().getHits().size();
        int guestHits = match.getOpponentPlayer().getHits().size();
        GameResults hostResult = resolveParticipantResult(match, MatchPlayerSide.HOST, surrenderSide);
        GameResults guestResult = resolveParticipantResult(match, MatchPlayerSide.OPPONENT, surrenderSide);

        GameFinishValidation.validateScores(hostHits, guestHits);
        validateFriendResults(hostHits, guestHits, hostResult, guestResult);

        HistoryGameModel game = new HistoryGameModel();
        game.setGameMode(GameModes.FRIEND);
        addProfilePlayer(game, match.getProfile(), USER_SLOT, hostHits, hostResult, match.getHostPlayer().getTurnTimeoutPenalties());
        addProfilePlayer(game, guest, OPPONENT_SLOT, guestHits, guestResult, match.getOpponentPlayer().getTurnTimeoutPenalties());
        return GameHistoryEntryDto.from(historyGameRepository.save(game));
    }

    private static GameResults resolveParticipantResult(
            ActiveMatchModel match,
            MatchPlayerSide side,
            MatchPlayerSide surrenderSide
    ) {
        if (surrenderSide == side || match.getBotReplacementSide() == side) {
            return GameResults.DESISTENCE;
        }
        if (match.getWinner() == null) {
            return GameResults.DRAW;
        }
        return match.getWinner() == side ? GameResults.WIN : GameResults.LOSE;
    }

    private static void validateFriendResults(
            int hostHits,
            int guestHits,
            GameResults hostResult,
            GameResults guestResult
    ) {
        if (hostResult == GameResults.DESISTENCE) {
            if (guestResult != GameResults.WIN) {
                throw new IllegalStateException("Resultado inconsistente após desistência do anfitrião.");
            }
            return;
        }
        if (guestResult == GameResults.DESISTENCE) {
            if (hostResult != GameResults.WIN) {
                throw new IllegalStateException("Resultado inconsistente após desistência do convidado.");
            }
            return;
        }
        GameFinishValidation.validateResult(hostHits, guestHits, hostResult);
        if (guestResult != GameFinishValidation.opponentResult(hostResult)) {
            throw new IllegalStateException("Resultados dos jogadores inconsistentes.");
        }
    }

    @Transactional(readOnly = true)
    public GameHistoryPageResponse listHistory(String userId, int page, int size) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "playedAt"));
        Page<HistoryGameModel> result = historyGameRepository.findByProfileId(profile.getId(), pageable);
        return GameHistoryPageResponse.from(result);
    }

    private void validateFinishRequest(GameFinishRequest request) {
        GameFinishValidation.validateResult(
                request.userCorrectGuesses(),
                request.opponentCorrectGuesses(),
                request.result()
        );
    }

    private HistoryGameModel buildFinishedGame(
            GameModes mode,
            String opponentName,
            ProfileModel userProfile,
            ProfileModel opponentProfile,
            GameFinishRequest request
    ) {
        HistoryGameModel game = new HistoryGameModel();
        game.setGameMode(mode);
        game.setOpponentName(opponentName);

        addProfilePlayer(
                game,
                userProfile,
                USER_SLOT,
                request.userCorrectGuesses(),
                request.result()
        );

        if (opponentProfile != null) {
            addProfilePlayer(
                    game,
                    opponentProfile,
                    OPPONENT_SLOT,
                    request.opponentCorrectGuesses(),
                    GameFinishValidation.opponentResult(request.result())
            );
        } else {
            addGuestPlayer(
                    game,
                    OPPONENT_SLOT,
                    request.opponentCorrectGuesses(),
                    GameFinishValidation.opponentResult(request.result())
            );
        }
        return game;
    }

    private void addProfilePlayer(
            HistoryGameModel game,
            ProfileModel profile,
            int slot,
            int correctGuesses,
            GameResults result,
            int turnTimeoutPenalties
    ) {
        HistoryGamePlayerModel player = new HistoryGamePlayerModel();
        player.setSlot(slot);
        player.setProfile(profile);
        player.setCorrectGuesses(correctGuesses);
        player.setResult(result);
        player.setTurnTimeoutPenalties(turnTimeoutPenalties);
        game.addPlayer(player);
    }

    private void addProfilePlayer(
            HistoryGameModel game,
            ProfileModel profile,
            int slot,
            int correctGuesses,
            GameResults result
    ) {
        addProfilePlayer(game, profile, slot, correctGuesses, result, 0);
    }

    private void addGuestPlayer(
            HistoryGameModel game,
            int slot,
            int correctGuesses,
            GameResults result
    ) {
        HistoryGamePlayerModel player = new HistoryGamePlayerModel();
        player.setSlot(slot);
        player.setProfile(null);
        player.setCorrectGuesses(correctGuesses);
        player.setResult(result);
        game.addPlayer(player);
    }
}
