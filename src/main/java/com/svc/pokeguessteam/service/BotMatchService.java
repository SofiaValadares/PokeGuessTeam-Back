package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchSetupResponse;
import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.GameBotFinishRequest;
import com.svc.pokeguessteam.dto.game.GameFinishResponse;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.dto.game.MatchRewardDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.util.GameConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public BotMatchService(
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

    /** Valida equipa no servidor; o jogo corre no cliente. */
    @Transactional
    public BotMatchSetupResponse validateTeamForClient(String userId, BotMatchTeamRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        clearStaleBotMatches(profile.getId());
        activeMatchConstraintService.ensureCanStartNewMatch(profile.getId());

        List<Integer> team = duelTeamService.validateTeamFromRegisteredPokedex(userId, request.team());
        int registeredCount = duelTeamService.countRegistered(userId);
        int minForBotDuel = GameConstants.TEAM_SIZE * 2;
        if (registeredCount < minForBotDuel) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_POKEDEX_INSUFFICIENT,
                    MessageKeys.GAME_POKEDEX_INSUFFICIENT,
                    minForBotDuel,
                    registeredCount
            );
        }
        Set<Integer> excluded = new HashSet<>(team);
        List<Integer> botTeam = duelTeamService.buildBotTeamFromUserPokedex(userId, excluded);
        ClientMatchCommitmentService.CommittedClientMatch committed =
                clientMatchCommitmentService.commit(profile, GameModes.BOT, "PokéBot", team, botTeam);
        return new BotMatchSetupResponse(
                committed.matchId(),
                committed.host().team(),
                committed.opponent().team(),
                committed.host().commitment(),
                committed.opponent().commitment()
        );
    }

    /** Abre os commitments (AES + SHA-256 + HMAC), persiste histórico e recompensas. */
    @Transactional
    public GameFinishResponse finishClientMatch(String userId, GameBotFinishRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ClientMatchCommitmentService.OpenedClientMatch opened = clientMatchCommitmentService.verifyAndConsume(
                profile.getId(),
                GameModes.BOT,
                request.matchId(),
                request.hostTeam(),
                request.opponentTeam()
        );
        GameHistoryEntryDto history = gameHistoryService.saveBotFinish(userId, request);
        MatchRewardDto reward = matchRewardService.grantForUser(userId, GameModes.BOT, request.result());
        return new GameFinishResponse(
                history,
                reward,
                opened.hostCommitment(),
                opened.opponentCommitment(),
                opened.hostOpening(),
                opened.opponentOpening()
        );
    }

    private void clearStaleBotMatches(String profileId) {
        activeMatchRepository.findAllActiveByProfileIdAndGameModeOrderByCreatedAtDesc(
                profileId,
                GameModes.BOT,
                MatchStatus.FINISHED
        ).forEach(match -> activeMatchRemovalService.deleteByMatchId(match.getId()));
    }
}
