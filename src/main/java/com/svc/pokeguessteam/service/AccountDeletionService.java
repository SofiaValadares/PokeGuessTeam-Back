package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.repository.auth.AuthCodeRepository;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.repository.game.HistoryGamePlayerRepository;
import com.svc.pokeguessteam.repository.user.FriendOnlinePenaltyRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokedexRepository;
import com.svc.pokeguessteam.repository.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ActiveMatchRepository activeMatchRepository;
    private final ActiveMatchRemovalService activeMatchRemovalService;
    private final HistoryGamePlayerRepository historyGamePlayerRepository;
    private final FriendOnlinePenaltyRepository friendOnlinePenaltyRepository;
    private final UserPokedexRepository userPokedexRepository;
    private final AuthCodeRepository authCodeRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountDeletionService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            ActiveMatchRepository activeMatchRepository,
            ActiveMatchRemovalService activeMatchRemovalService,
            HistoryGamePlayerRepository historyGamePlayerRepository,
            FriendOnlinePenaltyRepository friendOnlinePenaltyRepository,
            UserPokedexRepository userPokedexRepository,
            AuthCodeRepository authCodeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.activeMatchRepository = activeMatchRepository;
        this.activeMatchRemovalService = activeMatchRemovalService;
        this.historyGamePlayerRepository = historyGamePlayerRepository;
        this.friendOnlinePenaltyRepository = friendOnlinePenaltyRepository;
        this.userPokedexRepository = userPokedexRepository;
        this.authCodeRepository = authCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void deleteAccount(String userId, String password) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_USER_NOT_FOUND,
                        MessageKeys.PROFILE_USER_NOT_FOUND
                ));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiBusinessException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodes.AUTH_CURRENT_PASSWORD_WRONG,
                    MessageKeys.AUTH_CURRENT_PASSWORD_WRONG
            );
        }

        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElse(null);
        String profileId = profile != null ? profile.getId() : null;

        if (profileId != null) {
            detachGuestFromActiveMatches(profileId);
            deleteHostedActiveMatches(profileId);
            historyGamePlayerRepository.clearProfileReferences(profileId);
            friendOnlinePenaltyRepository.deleteByProfile_Id(profileId);
            userPokedexRepository.deleteByProfile_Id(profileId);
            profileRepository.deleteById(profileId);
            profileRepository.flush();
        }

        authCodeRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }

    private void deleteHostedActiveMatches(String profileId) {
        List<ActiveMatchModel> hosted = activeMatchRepository.findByProfile_IdAndStatusNot(
                profileId,
                MatchStatus.FINISHED
        );
        for (ActiveMatchModel match : hosted) {
            activeMatchRemovalService.deleteByMatchId(match.getId());
        }
    }

    private void detachGuestFromActiveMatches(String profileId) {
        List<ActiveMatchModel> guestMatches = new ArrayList<>(
                activeMatchRepository.findByGuestProfile_Id(profileId)
        );
        for (ActiveMatchModel match : guestMatches) {
            match.setGuestProfile(null);
        }
        if (!guestMatches.isEmpty()) {
            activeMatchRepository.saveAll(guestMatches);
        }
    }
}
