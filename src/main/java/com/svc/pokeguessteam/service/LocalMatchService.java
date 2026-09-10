package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.GameFinishResponse;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.GameLocalFinishRequest;
import com.svc.pokeguessteam.dto.game.LocalMatchSetupRequest;
import com.svc.pokeguessteam.dto.game.MatchRewardDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.util.GameFinishValidation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocalMatchService {

    private final ActiveMatchRepository activeMatchRepository;
    private final ProfileService profileService;
    private final GameHistoryService gameHistoryService;
    private final MatchRewardService matchRewardService;
    private final ActiveMatchConstraintService activeMatchConstraintService;
    private final DuelTeamService duelTeamService;
    private final ActiveMatchRemovalService activeMatchRemovalService;
    private final ActiveMatchTeamService activeMatchTeamService;

    public LocalMatchService(
            ActiveMatchRepository activeMatchRepository,
            ProfileService profileService,
            GameHistoryService gameHistoryService,
            MatchRewardService matchRewardService,
            ActiveMatchConstraintService activeMatchConstraintService,
            DuelTeamService duelTeamService,
            ActiveMatchRemovalService activeMatchRemovalService,
            ActiveMatchTeamService activeMatchTeamService
    ) {
        this.activeMatchRepository = activeMatchRepository;
        this.profileService = profileService;
        this.gameHistoryService = gameHistoryService;
        this.matchRewardService = matchRewardService;
        this.activeMatchConstraintService = activeMatchConstraintService;
        this.duelTeamService = duelTeamService;
        this.activeMatchRemovalService = activeMatchRemovalService;
        this.activeMatchTeamService = activeMatchTeamService;
    }

    /** Valida equipas e nome do adversário; o jogo corre no cliente. */
    @Transactional
    public void validateSetupForClient(String userId, LocalMatchSetupRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        clearStaleLocalMatches(profile.getId());
        activeMatchConstraintService.ensureCanStartNewMatch(profile.getId());
        GameFinishValidation.validateAndNormalizeLocalOpponentName(request.opponentName());
        duelTeamService.validateTeamFromRegisteredPokedex(userId, request.hostTeam());
        duelTeamService.validateTeamFromRegisteredPokedex(userId, request.opponentTeam());

        ActiveMatchModel match = createSetupMatch(profile, request.opponentName(), request.hostTeam(), request.opponentTeam());
        ActiveMatchModel saved = activeMatchRepository.save(match);
        activeMatchTeamService.saveTeams(saved);
    }

    /** Persiste histórico e recompensas após partida resolvida no cliente. */
    @Transactional
    public GameFinishResponse finishClientMatch(String userId, GameLocalFinishRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveSetupMatch(profile.getId(), GameModes.LOCAL);
        List<Integer> userTeam = activeMatchTeamService.loadTeam(match.getHostPlayer().getId());
        List<Integer> opponentTeam = activeMatchTeamService.loadTeam(match.getOpponentPlayer().getId());
        GameHistoryEntryDto history = gameHistoryService.saveLocalFinish(userId, request, userTeam, opponentTeam);
        activeMatchRemovalService.deleteByMatchId(match.getId());
        MatchRewardDto reward = matchRewardService.grantForUser(userId, GameModes.LOCAL, request.result());
        return new GameFinishResponse(history, reward);
    }

    private ActiveMatchModel createSetupMatch(
            ProfileModel profile,
            String opponentName,
            List<Integer> hostTeam,
            List<Integer> opponentTeam
    ) {
        ActiveMatchModel match = FriendMatchStore.newMatchShell();
        match.setProfile(profile);
        match.setGameMode(GameModes.LOCAL);
        match.setOpponentName(opponentName);
        match.getHostPlayer().setSide(MatchPlayerSide.HOST);
        match.getHostPlayer().setTeam(hostTeam);
        match.getOpponentPlayer().setSide(MatchPlayerSide.OPPONENT);
        match.getOpponentPlayer().setTeam(opponentTeam);
        return match;
    }

    private ActiveMatchModel requireActiveSetupMatch(String profileId, GameModes mode) {
        return activeMatchRepository.findAllUnfinishedForProfileOrderByCreatedAtDesc(profileId, MatchStatus.FINISHED)
                .stream()
                .filter(match -> match.getGameMode() == mode)
                .findFirst()
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                    ErrorCodes.GAME_MATCH_NOT_FOUND,
                    MessageKeys.GAME_MATCH_NOT_FOUND
                ));
    }

    private void clearStaleLocalMatches(String profileId) {
        activeMatchRepository.findAllActiveByProfileIdAndGameModeOrderByCreatedAtDesc(
                profileId,
                GameModes.LOCAL,
                MatchStatus.FINISHED
        ).forEach(match -> activeMatchRemovalService.deleteByMatchId(match.getId()));
    }
}
