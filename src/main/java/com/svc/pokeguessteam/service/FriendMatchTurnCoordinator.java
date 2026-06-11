package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.util.GameConstants;
import org.springframework.stereotype.Service;

/**
 * Temporizador de turno das partidas amigo (sem push em tempo real — o cliente faz polling HTTP).
 */
@Service
public class FriendMatchTurnCoordinator {

    private final FriendMatchService friendMatchService;
    private final MatchTurnTimerService turnTimerService;

    public FriendMatchTurnCoordinator(
            FriendMatchService friendMatchService,
            MatchTurnTimerService turnTimerService
    ) {
        this.friendMatchService = friendMatchService;
        this.turnTimerService = turnTimerService;
    }

    public void afterTeamReady(String matchId, String hostUserId, String guestUserId, FriendMatchStateDto hostView) {
        if (hostView.status() == MatchStatus.ACTIVE) {
            scheduleTimerIfNeeded(matchId, hostUserId, guestUserId);
        }
    }

    public void afterHumanGuess(String matchId, String hostUserId, String guestUserId, FriendMatchStateDto hostView) {
        turnTimerService.cancel(matchId);
        afterTurnChange(matchId, hostUserId, guestUserId, hostView);
    }

    public void afterSurrender(String matchId) {
        turnTimerService.cancel(matchId);
    }

    private void afterTurnChange(
            String matchId,
            String hostUserId,
            String guestUserId,
            FriendMatchStateDto hostView
    ) {
        if (hostView.status() == MatchStatus.FINISHED) {
            turnTimerService.cancel(matchId);
            return;
        }
        if (hostView.status() != MatchStatus.ACTIVE) {
            return;
        }
        scheduleTimerIfNeeded(matchId, hostUserId, guestUserId);
    }

    private void scheduleTimerIfNeeded(String matchId, String hostUserId, String guestUserId) {
        friendMatchService.armTurnDeadline(matchId);
        long sequence = friendMatchService.currentTurnSequence(matchId);

        turnTimerService.schedule(matchId, GameConstants.FRIEND_TURN_TIMEOUT_SECONDS, () ->
                handleTimeout(matchId, sequence, hostUserId, guestUserId)
        );
    }

    private void handleTimeout(String matchId, long expectedSequence, String hostUserId, String guestUserId) {
        FriendMatchService.TurnTimeoutStep step = friendMatchService.processTurnTimeout(matchId, expectedSequence);
        if (step == null) {
            return;
        }
        afterHumanGuess(matchId, hostUserId, guestUserId, step.hostView());
    }
}
