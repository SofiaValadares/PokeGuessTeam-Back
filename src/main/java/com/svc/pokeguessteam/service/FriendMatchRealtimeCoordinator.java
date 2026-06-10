package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.realtime.MatchRealtimeMessage;
import com.svc.pokeguessteam.realtime.MatchRealtimePublisher;
import com.svc.pokeguessteam.util.GameConstants;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FriendMatchRealtimeCoordinator {

    private final FriendMatchService friendMatchService;
    private final MatchRealtimePublisher publisher;
    private final MatchTurnTimerService turnTimerService;

    public FriendMatchRealtimeCoordinator(
            FriendMatchService friendMatchService,
            MatchRealtimePublisher publisher,
            MatchTurnTimerService turnTimerService
    ) {
        this.friendMatchService = friendMatchService;
        this.publisher = publisher;
        this.turnTimerService = turnTimerService;
    }

    public void publishAfterHumanGuess(
            String matchId,
            String hostUserId,
            String guestUserId,
            FriendMatchStateDto hostView,
            FriendMatchStateDto guestView,
            BotMatchGuessFeedbackDto feedback
    ) {
        turnTimerService.cancel(matchId);
        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.friendPlayerGuess(matchId, hostView, feedback),
                MatchRealtimeMessage.friendPlayerGuess(matchId, guestView, feedback)
        );
        afterTurnChange(matchId, hostUserId, guestUserId, hostView, guestView);
    }

    public void publishAfterGuestJoin(String matchId, String hostUserId, String guestUserId) {
        FriendMatchStateDto hostView = friendMatchService.getStateForUser(matchId, hostUserId);
        FriendMatchStateDto guestView = friendMatchService.getStateForUser(matchId, guestUserId);
        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.friendState(matchId, hostView),
                MatchRealtimeMessage.friendState(matchId, guestView)
        );
    }

    public void publishAfterTeamReady(
            String matchId,
            String hostUserId,
            String guestUserId,
            FriendMatchStateDto hostView,
            FriendMatchStateDto guestView
    ) {
        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.friendState(matchId, hostView),
                MatchRealtimeMessage.friendState(matchId, guestView)
        );
        if (hostView.status() == MatchStatus.ACTIVE) {
            scheduleTimerIfNeeded(matchId, hostUserId, guestUserId);
        }
    }

    public void publishAfterSurrender(
            String matchId,
            String hostUserId,
            String guestUserId,
            FriendMatchStateDto hostView,
            FriendMatchStateDto guestView
    ) {
        turnTimerService.cancel(matchId);
        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.finishedFriend(matchId, hostView),
                MatchRealtimeMessage.finishedFriend(matchId, guestView)
        );
    }

    public void afterTurnChange(
            String matchId,
            String hostUserId,
            String guestUserId,
            FriendMatchStateDto hostView,
            FriendMatchStateDto guestView
    ) {
        if (hostView.status() == MatchStatus.FINISHED) {
            turnTimerService.cancel(matchId);
            publisher.publishFriendToBoth(
                    matchId,
                    hostUserId,
                    guestUserId,
                    MatchRealtimeMessage.finishedFriend(matchId, hostView),
                    MatchRealtimeMessage.finishedFriend(matchId, guestView)
            );
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
        LocalDateTime deadline = friendMatchService.turnDeadline(matchId);

        FriendMatchStateDto hostView = friendMatchService.getStateForUser(matchId, hostUserId);
        FriendMatchStateDto guestView = friendMatchService.getStateForUser(matchId, guestUserId);

        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.friendTimer(
                        matchId,
                        hostView,
                        hostView.currentTurn(),
                        deadline,
                        GameConstants.FRIEND_TURN_TIMEOUT_SECONDS
                ),
                MatchRealtimeMessage.friendTimer(
                        matchId,
                        guestView,
                        guestView.currentTurn(),
                        deadline,
                        GameConstants.FRIEND_TURN_TIMEOUT_SECONDS
                )
        );

        turnTimerService.schedule(matchId, GameConstants.FRIEND_TURN_TIMEOUT_SECONDS, () ->
                handleTimeout(matchId, sequence, hostUserId, guestUserId)
        );
    }

    private void handleTimeout(String matchId, long expectedSequence, String hostUserId, String guestUserId) {
        FriendMatchService.TurnTimeoutStep step = friendMatchService.processTurnTimeout(matchId, expectedSequence);
        if (step == null) {
            return;
        }
        publishAfterTimeout(matchId, hostUserId, guestUserId, step);
    }

    private void publishAfterTimeout(
            String matchId,
            String hostUserId,
            String guestUserId,
            FriendMatchService.TurnTimeoutStep step
    ) {
        FriendMatchStateDto hostView = step.hostView();
        FriendMatchStateDto guestView = step.guestView();
        BotMatchGuessFeedbackDto feedback = step.feedback();

        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.friendPlayerGuess(matchId, hostView, feedback),
                MatchRealtimeMessage.friendPlayerGuess(matchId, guestView, feedback)
        );

        afterTurnChange(matchId, hostUserId, guestUserId, hostView, guestView);
    }
}
