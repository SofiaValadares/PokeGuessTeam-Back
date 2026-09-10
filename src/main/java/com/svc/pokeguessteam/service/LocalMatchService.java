package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.GameFinishResponse;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.GameLocalFinishRequest;
import com.svc.pokeguessteam.dto.game.LocalMatchSetupRequest;
import com.svc.pokeguessteam.dto.game.MatchRewardDto;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.util.GameFinishValidation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalMatchService {

    private final ActiveMatchRepository activeMatchRepository;
    private final ProfileService profileService;
    private final GameHistoryService gameHistoryService;
    private final MatchRewardService matchRewardService;
    private final ActiveMatchConstraintService activeMatchConstraintService;
    private final DuelTeamService duelTeamService;
    private final ActiveMatchRemovalService activeMatchRemovalService;

    public LocalMatchService(
            ActiveMatchRepository activeMatchRepository,
            ProfileService profileService,
            GameHistoryService gameHistoryService,
            MatchRewardService matchRewardService,
            ActiveMatchConstraintService activeMatchConstraintService,
            DuelTeamService duelTeamService,
            ActiveMatchRemovalService activeMatchRemovalService
    ) {
        this.activeMatchRepository = activeMatchRepository;
        this.profileService = profileService;
        this.gameHistoryService = gameHistoryService;
        this.matchRewardService = matchRewardService;
        this.activeMatchConstraintService = activeMatchConstraintService;
        this.duelTeamService = duelTeamService;
        this.activeMatchRemovalService = activeMatchRemovalService;
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
    }

    /** Persiste histórico e recompensas após partida resolvida no cliente. */
    @Transactional
    public GameFinishResponse finishClientMatch(String userId, GameLocalFinishRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        clearStaleLocalMatches(profile.getId());
        GameHistoryEntryDto history = gameHistoryService.saveLocalFinish(userId, request);
        MatchRewardDto reward = matchRewardService.grantForUser(userId, GameModes.LOCAL, request.result());
        return new GameFinishResponse(history, reward);
    }

    private void clearStaleLocalMatches(String profileId) {
        activeMatchRepository.findAllActiveByProfileIdAndGameModeOrderByCreatedAtDesc(
                profileId,
                GameModes.LOCAL,
                MatchStatus.FINISHED
        ).forEach(match -> activeMatchRemovalService.deleteByMatchId(match.getId()));
    }
}
