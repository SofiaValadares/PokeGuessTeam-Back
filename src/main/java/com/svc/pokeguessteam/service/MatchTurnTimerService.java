package com.svc.pokeguessteam.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class MatchTurnTimerService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "match-turn-timer");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, ScheduledFuture<?>> scheduledByMatch = new ConcurrentHashMap<>();

    public void schedule(String matchId, long delaySeconds, Runnable task) {
        cancel(matchId);
        ScheduledFuture<?> future = scheduler.schedule(
                () -> {
                    try {
                        task.run();
                    } finally {
                        scheduledByMatch.remove(matchId);
                    }
                },
                delaySeconds,
                TimeUnit.SECONDS
        );
        scheduledByMatch.put(matchId, future);
    }

    public void cancel(String matchId) {
        ScheduledFuture<?> future = scheduledByMatch.remove(matchId);
        if (future != null) {
            future.cancel(false);
        }
    }
}
