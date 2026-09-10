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
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
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
    private final ActiveMatchTeamService activeMatchTeamService;

    public BotMatchService(
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

        ActiveMatchModel match = createSetupMatch(profile, team, botTeam);
        ActiveMatchModel saved = activeMatchRepository.save(match);
        activeMatchTeamService.saveTeams(saved);
        return new BotMatchSetupResponse(team, botTeam);
    }

    /** Persiste histórico e recompensas após partida resolvida no cliente. */
    @Transactional
    public GameFinishResponse finishClientMatch(String userId, GameBotFinishRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        ActiveMatchModel match = requireActiveSetupMatch(profile.getId(), GameModes.BOT);
        List<Integer> userTeam = activeMatchTeamService.loadTeam(match.getHostPlayer().getId());
        List<Integer> opponentTeam = activeMatchTeamService.loadTeam(match.getOpponentPlayer().getId());
        GameHistoryEntryDto history = gameHistoryService.saveBotFinish(userId, request, userTeam, opponentTeam);
        activeMatchRemovalService.deleteByMatchId(match.getId());
        MatchRewardDto reward = matchRewardService.grantForUser(userId, GameModes.BOT, request.result());
        return new GameFinishResponse(history, reward);
    }

    private ActiveMatchModel createSetupMatch(ProfileModel profile, List<Integer> userTeam, List<Integer> opponentTeam) {
        ActiveMatchModel match = FriendMatchStore.newMatchShell();
        match.setProfile(profile);
        match.setGameMode(GameModes.BOT);
        match.getHostPlayer().setSide(MatchPlayerSide.HOST);
        match.getHostPlayer().setTeam(userTeam);
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

    private void clearStaleBotMatches(String profileId) {
        activeMatchRepository.findAllActiveByProfileIdAndGameModeOrderByCreatedAtDesc(
                profileId,
                GameModes.BOT,
                MatchStatus.FINISHED
        ).forEach(match -> activeMatchRemovalService.deleteByMatchId(match.getId()));
    }
}
