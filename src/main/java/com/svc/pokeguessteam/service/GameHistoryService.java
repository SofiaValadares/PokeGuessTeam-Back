package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.GameBotFinishRequest;
import com.svc.pokeguessteam.dto.game.GameFinishRequest;
import com.svc.pokeguessteam.dto.game.GameFriendFinishRequest;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.GameHistoryPageResponse;
import com.svc.pokeguessteam.dto.game.GameLocalFinishRequest;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.HistoryGameModel;
import com.svc.pokeguessteam.model.game.HistoryGamePlayerModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.HistoryGameRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.util.GameFinishValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameHistoryService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private static final int USER_SLOT = 1;
    private static final int OPPONENT_SLOT = 2;

    private final HistoryGameRepository historyGameRepository;
    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final UserRepository userRepository;

    public GameHistoryService(
            HistoryGameRepository historyGameRepository,
            ProfileRepository profileRepository,
            ProfileService profileService,
            UserRepository userRepository
    ) {
        this.historyGameRepository = historyGameRepository;
        this.profileRepository = profileRepository;
        this.profileService = profileService;
        this.userRepository = userRepository;
    }

    /**
     * Modo local (pass-and-play): adversário identificado por nome, sem perfil.
     */
    @Transactional
    public GameHistoryEntryDto saveLocalGame(String userId, GameLocalFinishRequest request) {
        String opponentName = GameFinishValidation.normalizeOpponentName(request.opponentName());
        GameFinishValidation.validateLocalOpponentName(opponentName);
        validateFinishRequest(request);

        ProfileModel userProfile = profileService.ensureProfileWithStarters(userId);
        HistoryGameModel game = buildFinishedGame(
                GameModes.LOCAL,
                opponentName,
                userProfile,
                null,
                request
        );
        return GameHistoryEntryDto.from(historyGameRepository.save(game));
    }

    /**
     * Persiste histórico a partir de partida local ativa (motor no servidor).
     */
    @Transactional
    public GameHistoryEntryDto saveLocalGameFromActiveMatch(
            ActiveMatchModel match,
            MatchPlayerSide surrenderSide
    ) {
        int userHits = match.getUserPlayer().getHits().size();
        int opponentHits = match.getBotPlayer().getHits().size();
        GameResults userResult = resolveLocalUserResult(match, surrenderSide);

        GameFinishValidation.validateScores(userHits, opponentHits);
        validateLocalActiveResult(userHits, opponentHits, userResult, surrenderSide);

        GameLocalFinishRequest request = new GameLocalFinishRequest(
                match.getOpponentName(),
                userHits,
                opponentHits,
                userResult
        );
        HistoryGameModel game = buildFinishedGame(
                GameModes.LOCAL,
                match.getOpponentName(),
                match.getProfile(),
                null,
                request
        );
        return GameHistoryEntryDto.from(historyGameRepository.save(game));
    }

    private static GameResults resolveLocalUserResult(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        if (surrenderSide == MatchPlayerSide.USER) {
            return GameResults.DESISTENCE;
        }
        if (surrenderSide == MatchPlayerSide.BOT) {
            return GameResults.WIN;
        }
        if (match.getWinner() == null) {
            return GameResults.DRAW;
        }
        return match.getWinner() == MatchPlayerSide.USER ? GameResults.WIN : GameResults.LOSE;
    }

    private static void validateLocalActiveResult(
            int userHits,
            int opponentHits,
            GameResults userResult,
            MatchPlayerSide surrenderSide
    ) {
        if (surrenderSide == MatchPlayerSide.USER || userResult == GameResults.DESISTENCE) {
            return;
        }
        if (surrenderSide == MatchPlayerSide.BOT) {
            if (userResult != GameResults.WIN) {
                throw new IllegalStateException("Resultado inconsistente após desistência do adversário local.");
            }
            return;
        }
        GameFinishValidation.validateResult(userHits, opponentHits, userResult);
    }

    /**
     * Modo bot: adversário artificial (slot 2 sem perfil).
     */
    @Transactional
    public GameHistoryEntryDto saveBotGame(String userId, GameBotFinishRequest request) {
        validateFinishRequest(request);

        ProfileModel userProfile = profileService.ensureProfileWithStarters(userId);
        HistoryGameModel game = buildFinishedGame(
                GameModes.BOT,
                null,
                userProfile,
                null,
                request
        );
        return GameHistoryEntryDto.from(historyGameRepository.save(game));
    }

    /**
     * Persiste histórico a partir de partida ativa vs bot (motor no servidor).
     */
    @Transactional
    public GameHistoryEntryDto saveBotGameFromActiveMatch(ActiveMatchModel match, boolean userSurrendered) {
        GameResults userResult = resolveUserResult(match, userSurrendered);
        GameBotFinishRequest request = new GameBotFinishRequest(
                match.getUserPlayer().getHits().size(),
                match.getBotPlayer().getHits().size(),
                userResult
        );
        validateFinishRequest(request);
        HistoryGameModel game = buildFinishedGame(
                GameModes.BOT,
                null,
                match.getProfile(),
                null,
                request
        );
        return GameHistoryEntryDto.from(historyGameRepository.save(game));
    }

    private static GameResults resolveUserResult(ActiveMatchModel match, boolean userSurrendered) {
        if (userSurrendered) {
            return GameResults.DESISTENCE;
        }
        if (match.getWinner() == null) {
            return GameResults.DRAW;
        }
        return match.getWinner() == MatchPlayerSide.USER ? GameResults.WIN : GameResults.LOSE;
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

        int hostHits = match.getUserPlayer().getHits().size();
        int guestHits = match.getBotPlayer().getHits().size();
        GameResults hostResult = resolveParticipantResult(match, MatchPlayerSide.USER, surrenderSide);
        GameResults guestResult = resolveParticipantResult(match, MatchPlayerSide.BOT, surrenderSide);

        GameFinishValidation.validateScores(hostHits, guestHits);
        validateFriendResults(hostHits, guestHits, hostResult, guestResult);

        HistoryGameModel game = new HistoryGameModel();
        game.setGameMode(GameModes.FRIEND);
        addProfilePlayer(game, match.getProfile(), USER_SLOT, hostHits, hostResult);
        addProfilePlayer(game, guest, OPPONENT_SLOT, guestHits, guestResult);
        return GameHistoryEntryDto.from(historyGameRepository.save(game));
    }

    private static GameResults resolveParticipantResult(
            ActiveMatchModel match,
            MatchPlayerSide side,
            MatchPlayerSide surrenderSide
    ) {
        if (surrenderSide == side) {
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

    /**
     * Modo amigo: dois perfis registados; um único registo visível nos históricos de ambos.
     */
    @Transactional
    public GameHistoryEntryDto saveFriendGame(String userId, GameFriendFinishRequest request) {
        if (userId.equals(request.opponentUserId())) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_CANNOT_PLAY_SELF,
                    MessageKeys.GAME_CANNOT_PLAY_SELF
            );
        }
        validateFinishRequest(request);

        ProfileModel userProfile = profileService.ensureProfileWithStarters(userId);
        ProfileModel opponentProfile = profileRepository.findByUser_IdUser(request.opponentUserId())
                .orElseGet(() -> {
                    if (!userRepository.existsById(request.opponentUserId())) {
                        throw new ApiBusinessException(
                                HttpStatus.NOT_FOUND,
                                ErrorCodes.GAME_OPPONENT_NOT_FOUND,
                                MessageKeys.GAME_OPPONENT_NOT_FOUND
                        );
                    }
                    return profileService.ensureProfileWithStarters(request.opponentUserId());
                });

        HistoryGameModel game = buildFinishedGame(
                GameModes.FRIEND,
                null,
                userProfile,
                opponentProfile,
                request
        );
        return GameHistoryEntryDto.from(historyGameRepository.save(game));
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
            GameResults result
    ) {
        HistoryGamePlayerModel player = new HistoryGamePlayerModel();
        player.setSlot(slot);
        player.setProfile(profile);
        player.setCorrectGuesses(correctGuesses);
        player.setResult(result);
        game.addPlayer(player);
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
