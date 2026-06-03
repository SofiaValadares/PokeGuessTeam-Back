package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.MatchRewardDto;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.util.GameMatchRewards;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchRewardService {

    private final ProfileService profileService;
    private final ActiveMatchRepository activeMatchRepository;

    public MatchRewardService(ProfileService profileService, ActiveMatchRepository activeMatchRepository) {
        this.profileService = profileService;
        this.activeMatchRepository = activeMatchRepository;
    }

    /**
     * Aplica recompensas do GDD e remove a partida ativa (já persistida no histórico).
     */
    @Transactional
    public MatchRewardDto grantAndRemoveActiveMatch(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        MatchRewardDto reward = switch (match.getGameMode()) {
            case BOT -> grantForProfile(
                    match.getProfile(),
                    resolveBotResult(match, surrenderSide == MatchPlayerSide.USER)
            );
            case LOCAL -> grantForProfile(
                    match.getProfile(),
                    resolveLocalResult(match, surrenderSide)
            );
            case FRIEND -> grantFriendMatch(match, surrenderSide);
        };
        activeMatchRepository.delete(match);
        return reward;
    }

    private MatchRewardDto grantFriendMatch(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        GameResults hostResult = resolveParticipantResult(match, MatchPlayerSide.USER, surrenderSide);
        MatchRewardDto hostReward = grantForProfile(match.getProfile(), hostResult);
        if (match.getGuestProfile() != null) {
            GameResults guestResult = resolveParticipantResult(match, MatchPlayerSide.BOT, surrenderSide);
            grantForProfile(match.getGuestProfile(), guestResult);
        }
        return hostReward;
    }

    private MatchRewardDto grantForProfile(ProfileModel profile, GameResults result) {
        String userId = profile.getUser().getIdUser();
        int xp = GameMatchRewards.xpForResult(result);
        int balls = GameMatchRewards.pokeBallsForResult(result);
        profileService.grantTrainingTeamMatchXp(userId, xp);
        if (balls > 0) {
            profileService.addPokeballs(userId, PokeballType.POKE_BALL, balls);
        }
        return new MatchRewardDto(xp, balls);
    }

    private static GameResults resolveBotResult(ActiveMatchModel match, boolean userSurrendered) {
        if (userSurrendered) {
            return GameResults.DESISTENCE;
        }
        if (match.getWinner() == null) {
            return GameResults.DRAW;
        }
        return match.getWinner() == MatchPlayerSide.USER ? GameResults.WIN : GameResults.LOSE;
    }

    private static GameResults resolveLocalResult(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
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
}
