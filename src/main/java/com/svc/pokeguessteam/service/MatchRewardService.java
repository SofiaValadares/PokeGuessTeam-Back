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
    private final FriendMatchStore friendMatchStore;

    public MatchRewardService(ProfileService profileService, FriendMatchStore friendMatchStore) {
        this.profileService = profileService;
        this.friendMatchStore = friendMatchStore;
    }

    @Transactional
    public MatchRewardDto grantForUser(String userId, GameModes mode, GameResults result) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        return grantForProfile(profile, mode, result);
    }

    /**
     * Recompensa prevista para um jogador (sem persistir).
     */
    public MatchRewardDto previewRewardForProfile(
            ActiveMatchModel match,
            ProfileModel profile,
            MatchPlayerSide surrenderSide
    ) {
        MatchPlayerSide side = resolveParticipantSide(match, profile);
        GameResults result = resolveParticipantResult(match, side, surrenderSide);
        return toRewardDto(GameMatchRewards.payout(GameModes.FRIEND, result));
    }

    @Transactional
    public MatchRewardDto grantAndRemoveActiveMatch(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        return grantAndRemoveActiveMatch(match, surrenderSide, null);
    }

    /**
     * Aplica recompensas do GDD e remove a partida ativa (já persistida no histórico).
     *
     * @param rewardForUserId utilizador que recebe o {@link MatchRewardDto} na resposta HTTP; null em eventos assíncronos.
     */
    @Transactional
    public MatchRewardDto grantAndRemoveActiveMatch(
            ActiveMatchModel match,
            MatchPlayerSide surrenderSide,
            String rewardForUserId
    ) {
        if (match.getGameMode() != GameModes.FRIEND) {
            throw new IllegalStateException("Partida ativa inesperada: " + match.getGameMode());
        }

        return friendMatchStore.completeOnce(match.getId(), () -> {
            if (!friendMatchStore.exists(match.getId())) {
                return emptyReward();
            }
            GrantedFriendRewards granted = grantFriendMatch(match, surrenderSide);
            friendMatchStore.remove(match.getId());
            if (rewardForUserId == null) {
                return emptyReward();
            }
            if (match.getProfile().getUser().getIdUser().equals(rewardForUserId)) {
                return granted.hostReward();
            }
            if (match.getGuestProfile() != null
                    && match.getGuestProfile().getUser().getIdUser().equals(rewardForUserId)) {
                return granted.guestReward();
            }
            return emptyReward();
        });
    }

    private record GrantedFriendRewards(MatchRewardDto hostReward, MatchRewardDto guestReward) {
    }

    private GrantedFriendRewards grantFriendMatch(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        GameResults hostResult = resolveParticipantResult(match, MatchPlayerSide.HOST, surrenderSide);
        MatchRewardDto hostReward = grantForProfile(match.getProfile(), GameModes.FRIEND, hostResult);
        MatchRewardDto guestReward = emptyReward();
        if (match.getGuestProfile() != null) {
            GameResults guestResult = resolveParticipantResult(match, MatchPlayerSide.OPPONENT, surrenderSide);
            guestReward = grantForProfile(match.getGuestProfile(), GameModes.FRIEND, guestResult);
        }
        return new GrantedFriendRewards(hostReward, guestReward);
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
        return toRewardDto(payout);
    }

    private static MatchRewardDto toRewardDto(GameMatchRewards.MatchRewardPayout payout) {
        return new MatchRewardDto(
                payout.trainingTeamXp(),
                payout.pokeBalls(),
                payout.pokeballFragments()
        );
    }

    private static MatchRewardDto emptyReward() {
        return new MatchRewardDto(0, 0, 0);
    }

    private static MatchPlayerSide resolveParticipantSide(ActiveMatchModel match, ProfileModel profile) {
        if (match.getProfile().getId().equals(profile.getId())) {
            return MatchPlayerSide.HOST;
        }
        if (match.getGuestProfile() != null && match.getGuestProfile().getId().equals(profile.getId())) {
            return MatchPlayerSide.OPPONENT;
        }
        throw new IllegalStateException("Perfil não pertence à partida.");
    }

    static GameResults resolveParticipantResult(
            ActiveMatchModel match,
            MatchPlayerSide side,
            MatchPlayerSide surrenderSide
    ) {
        if (surrenderSide != null) {
            if (surrenderSide == side) {
                return GameResults.DESISTENCE;
            }
            if (match.getWinner() == null) {
                return GameResults.DRAW;
            }
            return match.getWinner() == side ? GameResults.WIN : GameResults.LOSE;
        }
        if (match.getBotReplacementSide() == side) {
            return GameResults.DESISTENCE;
        }
        if (match.getWinner() == null) {
            return GameResults.DRAW;
        }
        return match.getWinner() == side ? GameResults.WIN : GameResults.LOSE;
    }
}
