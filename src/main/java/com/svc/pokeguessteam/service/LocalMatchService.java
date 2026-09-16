package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.GameFinishResponse;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.GameLocalFinishRequest;
import com.svc.pokeguessteam.dto.game.LocalMatchSetupRequest;
import com.svc.pokeguessteam.dto.game.LocalMatchSetupResponse;
import com.svc.pokeguessteam.dto.game.MatchRewardDto;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.util.GameFinishValidation;
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
    private final ClientMatchCommitmentService clientMatchCommitmentService;

    public LocalMatchService(
            ActiveMatchRepository activeMatchRepository,
            ProfileService profileService,
            GameHistoryService gameHistoryService,
            MatchRewardService matchRewardService,
            ActiveMatchConstraintService activeMatchConstraintService,
            DuelTeamService duelTeamService,
            ActiveMatchRemovalService activeMatchRemovalService,
            ClientMatchCommitmentService clientMatchCommitmentService
    ) {
        this.activeMatchRepository = activeMatchRepository;
        this.profileService = profileService;
        this.gameHistoryService = gameHistoryService;
        this.matchRewardService = matchRewardService;
        this.activeMatchConstraintService = activeMatchConstraintService;
        this.duelTeamService = duelTeamService;
        this.activeMatchRemovalService = activeMatchRemovalService;
        this.clientMatchCommitmentService = clientMatchCommitmentService;
    }

    /** Valida equipas, publica commitments e sela as aberturas. */
    @Transactional
    public LocalMatchSetupResponse validateSetupForClient(String userId, LocalMatchSetupRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        clearStaleLocalMatches(profile.getId());
        activeMatchConstraintService.ensureCanStartNewMatch(profile.getId());
        String opponentName = GameFinishValidation.validateAndNormalizeLocalOpponentName(request.opponentName());
        List<Integer> hostTeam = duelTeamService.validateTeamFromRegisteredPokedex(userId, request.hostTeam());
        List<Integer> opponentTeam = duelTeamService.validateTeamFromRegisteredPokedex(userId, request.opponentTeam());
        ClientMatchCommitmentService.CommittedClientMatch committed =
                clientMatchCommitmentService.commit(profile, GameModes.LOCAL, opponentName, hostTeam, opponentTeam);
        return new LocalMatchSetupResponse(
                committed.matchId(),
                committed.host().commitment(),
                committed.opponent().commitment()
        );
    }

    /** Abre os commitments (AES + SHA-256 + HMAC), persiste histórico e recompensas. */
    @Transactional
    public GameFinishResponse finishClientMatch(String userId, GameLocalFinishRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ClientMatchCommitmentService.OpenedClientMatch opened = clientMatchCommitmentService.verifyAndConsume(
                profile.getId(),
                GameModes.LOCAL,
                request.matchId(),
                request.hostTeam(),
                request.opponentTeam()
        );
        GameHistoryEntryDto history = gameHistoryService.saveLocalFinish(userId, request);
        MatchRewardDto reward = matchRewardService.grantForUser(userId, GameModes.LOCAL, request.result());
        return new GameFinishResponse(
                history,
                reward,
                opened.hostCommitment(),
                opened.opponentCommitment(),
                opened.hostOpening(),
                opened.opponentOpening()
        );
    }

    private void clearStaleLocalMatches(String profileId) {
        activeMatchRepository.findAllActiveByProfileIdAndGameModeOrderByCreatedAtDesc(
                profileId,
                GameModes.LOCAL,
                MatchStatus.FINISHED
        ).forEach(match -> activeMatchRemovalService.deleteByMatchId(match.getId()));
    }
}
