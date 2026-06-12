package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.realtime.MatchRealtimeMessage;
import com.svc.pokeguessteam.realtime.MatchRealtimePublisher;
import org.springframework.stereotype.Service;

@Service
public class FriendMatchRealtimeCoordinator {

    private final MatchRealtimePublisher publisher;

    public FriendMatchRealtimeCoordinator(MatchRealtimePublisher publisher) {
        this.publisher = publisher;
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
    }

    public void publishAfterHumanGuess(
            String matchId,
            String hostUserId,
            String guestUserId,
            FriendMatchStateDto hostView,
            FriendMatchStateDto guestView,
            BotMatchGuessFeedbackDto feedback
    ) {
        publisher.publishFriendToBoth(
                matchId,
                hostUserId,
                guestUserId,
                MatchRealtimeMessage.friendPlayerGuess(matchId, hostView, feedback),
                MatchRealtimeMessage.friendPlayerGuess(matchId, guestView, feedback)
        );
        if (hostView.status() == MatchStatus.FINISHED) {
            publisher.publishFriendToBoth(
                    matchId,
                    hostUserId,
                    guestUserId,
                    MatchRealtimeMessage.finishedFriend(matchId, hostView),
                    MatchRealtimeMessage.finishedFriend(matchId, guestView)
            );
        }
    }

    public void publishAfterSurrender(
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
                MatchRealtimeMessage.finishedFriend(matchId, hostView),
                MatchRealtimeMessage.finishedFriend(matchId, guestView)
        );
    }
}
