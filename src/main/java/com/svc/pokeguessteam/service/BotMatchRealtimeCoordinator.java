package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchActionResponse;
import com.svc.pokeguessteam.dto.game.BotMatchGuessFeedbackDto;
import com.svc.pokeguessteam.dto.game.BotMatchStateDto;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.realtime.MatchRealtimeMessage;
import com.svc.pokeguessteam.realtime.MatchRealtimePublisher;
import com.svc.pokeguessteam.util.GameConstants;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BotMatchRealtimeCoordinator {

    private final BotMatchService botMatchService;
    private final MatchRealtimePublisher publisher;

    public BotMatchRealtimeCoordinator(BotMatchService botMatchService, MatchRealtimePublisher publisher) {
        this.botMatchService = botMatchService;
        this.publisher = publisher;
    }

    public void publishAfterHostGuess(BotMatchActionResponse response) {
        String matchId = response.match().matchId();
        publisher.publishBot(
                matchId,
                MatchRealtimeMessage.botPlayerGuess(matchId, response.match(), firstFeedback(response))
        );

        if (response.match().status() == MatchStatus.FINISHED) {
            publisher.publishBot(matchId, MatchRealtimeMessage.finishedBot(matchId, response.match()));
            return;
        }

        if (response.match().currentTurn() == MatchPlayerSide.OPPONENT) {
            publisher.publishBot(matchId, MatchRealtimeMessage.botTurnStart(matchId));
            runBotSequenceAsync(matchId);
        } else {
            publisher.publishBot(matchId, MatchRealtimeMessage.botState(matchId, response.match()));
        }
    }

    public void publishAfterTeamSubmit(BotMatchActionResponse response) {
        String matchId = response.match().matchId();
        publisher.publishBot(matchId, MatchRealtimeMessage.botState(matchId, response.match()));

        if (response.match().status() == MatchStatus.FINISHED) {
            publisher.publishBot(matchId, MatchRealtimeMessage.finishedBot(matchId, response.match()));
            return;
        }

        if (response.match().currentTurn() == MatchPlayerSide.OPPONENT) {
            publisher.publishBot(matchId, MatchRealtimeMessage.botTurnStart(matchId));
            runBotSequenceAsync(matchId);
        }
    }

    @Async
    public void runBotSequenceAsync(String matchId) {
        int safety = 50;
        while (safety-- > 0) {
            sleep();
            BotMatchService.BotTurnStep step = botMatchService.processSingleBotTurn(matchId);
            if (step == null) {
                break;
            }
            publisher.publishBot(
                    matchId,
                    MatchRealtimeMessage.botGuess(matchId, step.state(), step.feedback())
            );
            if (step.state().status() == MatchStatus.FINISHED) {
                publisher.publishBot(matchId, MatchRealtimeMessage.finishedBot(matchId, step.state()));
                return;
            }
            if (step.state().currentTurn() != MatchPlayerSide.OPPONENT) {
                publisher.publishBot(matchId, MatchRealtimeMessage.botState(matchId, step.state()));
                return;
            }
        }
        BotMatchStateDto state = botMatchService.getStateByMatchId(matchId);
        if (state != null) {
            publisher.publishBot(matchId, MatchRealtimeMessage.botState(matchId, state));
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(GameConstants.BOT_GUESS_WS_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static BotMatchGuessFeedbackDto firstFeedback(BotMatchActionResponse response) {
        if (response.turnFeedbacks() == null || response.turnFeedbacks().isEmpty()) {
            return null;
        }
        return response.turnFeedbacks().get(0);
    }
}
