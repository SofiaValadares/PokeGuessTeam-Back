package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.MatchRewardDto;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MatchRewardServiceTest {

    @Test
    void cancelledMatchCompletesWithoutGrantingRewards() {
        ProfileService profileService = mock(ProfileService.class);
        MatchRewardService service = new MatchRewardService(profileService, new FriendMatchStore());
        ActiveMatchModel match = new ActiveMatchModel();
        match.setId("cancelled-match");
        match.setGameMode(GameModes.FRIEND);

        MatchRewardDto reward = service.completeCancelledMatch(match);

        assertEquals(new MatchRewardDto(0, 0, 0), reward);
        verifyNoInteractions(profileService);
    }
}