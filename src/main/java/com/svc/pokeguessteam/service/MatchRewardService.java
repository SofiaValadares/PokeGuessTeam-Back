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
    private final BonusEventService bonusEventService;

    public MatchRewardService(
            ProfileService profileService,
            FriendMatchStore friendMatchStore,
            BonusEventService bonusEventService
    ) {
        this.profileService = profileService;
        this.friendMatchStore = friendMatchStore;
        this.bonusEventService = bonusEventService;
    }

    @Transactional
    public MatchRewardDto grantForUser(String userId, GameModes mode, GameResults result) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        return grantForProfile(profile, mode, result, false);
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
        GameModes mode = onlineRewardMode(match);
        GameMatchRewards.MatchRewardPayout payout = GameMatchRewards.payout(mode, result);
        double multiplier = match.isEventMode()
                ? bonusEventService.resolveActiveEventXpMultiplier()
                : bonusEventService.resolveXpMultiplier(profile);
        int xp = (int) Math.round(payout.trainingTeamXp() * multiplier);
        PokeballType ballType = resolveGrantedBallType(payout.pokeBalls());
        return MatchRewardDto.of(
                xp,
                payout.pokeBalls(),
                payout.pokeballFragments(),
                ballType != null ? ballType.name() : null
        );
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
        if (match.getGameMode() != GameModes.FRIEND && match.getGameMode() != GameModes.COMPETITIVE) {
            throw new IllegalStateException("Partida ativa inesperada: " + match.getGameMode());
        }

        return friendMatchStore.completeOnce(match.getId(), () -> {
            if (!friendMatchStore.exists(match.getId())) {
                return MatchRewardDto.empty();
            }
            GrantedFriendRewards granted = grantOnlineMatch(match, surrenderSide);
            if (rewardForUserId == null) {
                return MatchRewardDto.empty();
            }
            if (match.getProfile().getUser().getIdUser().equals(rewardForUserId)) {
                return granted.hostReward();
            }
            if (match.getGuestProfile() != null
                    && match.getGuestProfile().getUser().getIdUser().equals(rewardForUserId)) {
                return granted.guestReward();
            }
            return MatchRewardDto.empty();
        });
    }

    private record GrantedFriendRewards(MatchRewardDto hostReward, MatchRewardDto guestReward) {
    }

    private GrantedFriendRewards grantOnlineMatch(ActiveMatchModel match, MatchPlayerSide surrenderSide) {
        boolean eventMatch = match.isEventMode();
        GameModes mode = onlineRewardMode(match);
        GameResults hostResult = resolveParticipantResult(match, MatchPlayerSide.HOST, surrenderSide);
        MatchRewardDto hostReward = grantForProfile(match.getProfile(), mode, hostResult, eventMatch);
        MatchRewardDto guestReward = MatchRewardDto.empty();
        if (match.getGuestProfile() != null) {
            GameResults guestResult = resolveParticipantResult(match, MatchPlayerSide.OPPONENT, surrenderSide);
            guestReward = grantForProfile(match.getGuestProfile(), mode, guestResult, eventMatch);
        }
        return new GrantedFriendRewards(hostReward, guestReward);
    }

    private static GameModes onlineRewardMode(ActiveMatchModel match) {
        return match.getGameMode() == GameModes.COMPETITIVE ? GameModes.COMPETITIVE : GameModes.FRIEND;
    }

    /**
     * @param forceEventXpMultiplier se true (partida de evento), aplica o ×XP do evento ativo sem exigir time de treino.
     */
    private MatchRewardDto grantForProfile(
            ProfileModel profile,
            GameModes mode,
            GameResults result,
            boolean forceEventXpMultiplier
    ) {
        String userId = profile.getUser().getIdUser();
        GameMatchRewards.MatchRewardPayout payout = GameMatchRewards.payout(mode, result);
        double multiplier = forceEventXpMultiplier
                ? bonusEventService.resolveActiveEventXpMultiplier()
                : bonusEventService.resolveXpMultiplier(profile);
        int trainingTeamXp = (int) Math.round(payout.trainingTeamXp() * multiplier);
        profileService.grantTrainingTeamMatchXp(userId, trainingTeamXp);

        PokeballType ballType = resolveGrantedBallType(payout.pokeBalls());
        if (ballType != null) {
            profileService.addPokeballs(userId, ballType, payout.pokeBalls());
        }
        if (payout.pokeballFragments() > 0) {
            profileService.addPokeballFragments(userId, payout.pokeballFragments());
        }
        return MatchRewardDto.of(
                trainingTeamXp,
                payout.pokeBalls(),
                payout.pokeballFragments(),
                ballType != null ? ballType.name() : null
        );
    }

    /** Com evento ativo, a vitória online dá Friend Ball em vez da Pokébola normal. */
    private PokeballType resolveGrantedBallType(int pokeBalls) {
        if (pokeBalls <= 0) {
            return null;
        }
        return bonusEventService.findActive().isPresent()
                ? PokeballType.FRIEND_BALL
                : PokeballType.POKE_BALL;
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
