package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.MatchRewardDto;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.util.GameMatchRewards;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchRewardService {

    private final ProfileService profileService;
    private final ActiveMatchRemovalService activeMatchRemovalService;

    public MatchRewardService(
            ProfileService profileService,
            ActiveMatchRemovalService activeMatchRemovalService
    ) {
        this.profileService = profileService;
        this.activeMatchRemovalService = activeMatchRemovalService;
    }

    @Transactional
    public MatchRewardDto grantForUser(String userId, GameModes mode, GameResults result) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        return grantForProfile(profile, mode, result);
    }

    /**
     * Aplica recompensas do GDD e remove a partida ativa (já persistida no histórico).
     */
    @Transactional
    public MatchRewardDto grantAndRemoveActiveMatch(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        if (match.getGameMode() != GameModes.FRIEND) {
            throw new IllegalStateException("Partida ativa inesperada: " + match.getGameMode());
        }
        MatchRewardDto reward = grantFriendMatch(match, surrenderSide);
        activeMatchRemovalService.deleteByMatchId(match.getId());
        return reward;
    }

    private MatchRewardDto grantFriendMatch(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        GameResults hostResult = resolveParticipantResult(match, MatchPlayerSide.HOST, surrenderSide);
        MatchRewardDto hostReward = grantForProfile(match.getProfile(), GameModes.FRIEND, hostResult);
        if (match.getGuestProfile() != null) {
            GameResults guestResult = resolveParticipantResult(match, MatchPlayerSide.OPPONENT, surrenderSide);
            grantForProfile(match.getGuestProfile(), GameModes.FRIEND, guestResult);
        }
        return hostReward;
    }

    private MatchRewardDto grantForProfile(ProfileModel profile, GameModes mode, GameResults result) {
        String userId = profile.getUser().getIdUser();
        GameMatchRewards.MatchRewardPayout payout = GameMatchRewards.payout(mode, result);
        profileService.grantTrainingTeamMatchXp(userId, payout.trainingTeamXp());
        if (payout.pokeBalls() > 0) {
            profileService.addPokeballs(userId, PokeballType.POKE_BALL, payout.pokeBalls());
        }
        if (payout.pokeballFragments() > 0) {
            profileService.addPokeballFragments(userId, payout.pokeballFragments());
        }
        return new MatchRewardDto(
                payout.trainingTeamXp(),
                payout.pokeBalls(),
                payout.pokeballFragments()
        );
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
