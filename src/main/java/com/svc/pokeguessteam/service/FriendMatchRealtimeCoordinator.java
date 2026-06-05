package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.FriendMatchActionResponse;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.realtime.MatchRealtimeMessage;
import com.svc.pokeguessteam.realtime.MatchRealtimePublisher;
import com.svc.pokeguessteam.util.GameConstants;
import org.springframework.scheduling.annotation.Async;
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

    public void publishAfterHumanGuess(FriendMatchActionResponse response, String hostUserId, String guestUserId) {
        String matchId = response.match().matchId();
        turnTimerService.cancel(matchId);
        FriendMatchStateDto hostView = friendMatchService.getStateForUser(matchId, hostUserId);
        FriendMatchStateDto guestView = friendMatchService.getStateForUser(matchId, guestUserId);
        BotMatchGuessFeedbackDto feedback = firstFeedback(response);

        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.friendPlayerGuess(matchId, hostView, feedback),
                MatchRealtimeMessage.friendPlayerGuess(matchId, guestView, feedback)
        );
        afterTurnChange(matchId, hostUserId, guestUserId, hostView);
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

    public void afterTurnChange(String matchId, String hostUserId, String guestUserId, FriendMatchStateDto anyView) {
        if (anyView.status() == MatchStatus.FINISHED) {
            turnTimerService.cancel(matchId);
            FriendMatchStateDto hostView = friendMatchService.getStateForUser(matchId, hostUserId);
            FriendMatchStateDto guestView = friendMatchService.getStateForUser(matchId, guestUserId);
            publisher.publishFriendToBoth(
                    matchId,
                    hostUserId,
                    guestUserId,
                    MatchRealtimeMessage.finishedFriend(matchId, hostView),
                    MatchRealtimeMessage.finishedFriend(matchId, guestView)
            );
            return;
        }

        if (anyView.status() != MatchStatus.ACTIVE) {
            return;
        }

        if (anyView.currentTurn() != null && friendMatchService.isBotControlledTurn(matchId, anyView.currentTurn())) {
            turnTimerService.cancel(matchId);
            runBotReplacementTurnAsync(matchId, hostUserId, guestUserId);
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
        FriendMatchActionResponse response = friendMatchService.processTurnTimeout(matchId, expectedSequence);
        if (response == null) {
            return;
        }
        publishAfterTimeout(response, hostUserId, guestUserId);
    }

    private void publishAfterTimeout(FriendMatchActionResponse response, String hostUserId, String guestUserId) {
        String matchId = response.match().matchId();
        FriendMatchStateDto hostView = friendMatchService.getStateForUser(matchId, hostUserId);
        FriendMatchStateDto guestView = friendMatchService.getStateForUser(matchId, guestUserId);

        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.timeoutPenalty(
                        matchId,
                        hostView,
                        hostView.yourTimeoutPenalties(),
                        GameConstants.FRIEND_MAX_TIMEOUT_PENALTIES_PER_MATCH
                ),
                MatchRealtimeMessage.timeoutPenalty(
                        matchId,
                        guestView,
                        guestView.yourTimeoutPenalties(),
                        GameConstants.FRIEND_MAX_TIMEOUT_PENALTIES_PER_MATCH
                )
        );

        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.friendPlayerGuess(matchId, hostView, firstFeedback(response)),
                MatchRealtimeMessage.friendPlayerGuess(matchId, guestView, firstFeedback(response))
        );

        if (friendMatchService.wasBotReplacementTriggered(matchId)) {
            publisher.publishFriendToBoth(
                    matchId,
                    hostUserId,
                    guestUserId,
                    MatchRealtimeMessage.opponentReplacedByBot(matchId, hostView),
                    MatchRealtimeMessage.opponentReplacedByBot(matchId, guestView)
            );
        }

        afterTurnChange(matchId, hostUserId, guestUserId, hostView);
    }

    @Async
    public void runBotReplacementTurnAsync(String matchId, String hostUserId, String guestUserId) {
        int safety = 50;
        while (safety-- > 0) {
            sleep();
            FriendMatchService.BotReplacementStep step = friendMatchService.processSingleBotReplacementTurn(matchId);
            if (step == null) {
                break;
            }
            publisher.publishFriendToBoth(
                    matchId,
                    hostUserId,
                    guestUserId,
                    MatchRealtimeMessage.friendPlayerGuess(matchId, step.hostView(), step.feedback()),
                    MatchRealtimeMessage.friendPlayerGuess(matchId, step.guestView(), step.feedback())
            );
            if (step.hostView().status() == MatchStatus.FINISHED) {
                publisher.publishFriendToBoth(
                        matchId,
                        hostUserId,
                        guestUserId,
                        MatchRealtimeMessage.finishedFriend(matchId, step.hostView()),
                        MatchRealtimeMessage.finishedFriend(matchId, step.guestView())
                );
                return;
            }
            if (!friendMatchService.isBotControlledTurn(matchId, step.hostView().currentTurn())) {
                afterTurnChange(matchId, hostUserId, guestUserId, step.hostView());
                return;
            }
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(GameConstants.BOT_GUESS_WS_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static BotMatchGuessFeedbackDto firstFeedback(FriendMatchActionResponse response) {
        if (response.turnFeedbacks() == null || response.turnFeedbacks().isEmpty()) {
            return null;
        }
        return response.turnFeedbacks().get(0);
    }
}
