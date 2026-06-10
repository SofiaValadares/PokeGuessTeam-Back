package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.user.FriendOnlinePenaltyModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.user.FriendOnlinePenaltyRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.util.GameConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FriendOnlineModerationService {

    private final FriendOnlinePenaltyRepository penaltyRepository;
    private final ProfileRepository profileRepository;

    public FriendOnlineModerationService(
            FriendOnlinePenaltyRepository penaltyRepository,
            ProfileRepository profileRepository
    ) {
        this.penaltyRepository = penaltyRepository;
        this.profileRepository = profileRepository;
    }

    public void ensureNotBanned(ProfileModel profile) {
        if (profile.isFriendOnlineBanned()) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.GAME_FRIEND_ONLINE_BANNED,
                    MessageKeys.GAME_FRIEND_ONLINE_BANNED
            );
        }
    }

    @Transactional
    public int recordTimeoutPenalty(ProfileModel profile, ActiveMatchModel match) {
        FriendOnlinePenaltyModel penalty = new FriendOnlinePenaltyModel();
        penalty.setProfile(profile);
        penaltyRepository.save(penalty);

        long recentCount = penaltyRepository.countByProfile_IdAndOccurredAtAfter(
                profile.getId(),
                LocalDateTime.now().minusHours(1)
        );
        if (recentCount >= GameConstants.FRIEND_TIMEOUT_BAN_THRESHOLD) {
            profile.setFriendOnlineBannedUntil(
                    LocalDateTime.now().plusHours(GameConstants.FRIEND_TIMEOUT_BAN_HOURS)
            );
            profileRepository.save(profile);
        }
        return (int) recentCount;
    }

    @Transactional(readOnly = true)
    public List<FriendOnlinePenaltyModel> recentPenalties(String profileId) {
        return penaltyRepository.findRecentByProfileId(
                profileId,
                LocalDateTime.now().minusDays(GameConstants.FRIEND_PENALTY_PROFILE_RETENTION_DAYS)
        );
    }
}
