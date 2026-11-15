package com.svc.pokeguessteam.job;

import com.svc.pokeguessteam.service.BonusEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BonusEventExpiryJob {

    private final BonusEventService bonusEventService;

    public BonusEventExpiryJob(BonusEventService bonusEventService) {
        this.bonusEventService = bonusEventService;
    }

    @Scheduled(fixedRate = 60_000)
    public void endExpiredEvents() {
        bonusEventService.endExpired();
    }
}
