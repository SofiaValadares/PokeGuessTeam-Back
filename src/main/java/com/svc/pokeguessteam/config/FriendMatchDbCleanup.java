package com.svc.pokeguessteam.config;

import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import com.svc.pokeguessteam.service.ActiveMatchRemovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Remove partidas amigo legadas da BD — o modo amigo usa apenas memória enquanto ativo.
 */
@Component
public class FriendMatchDbCleanup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FriendMatchDbCleanup.class);

    private final ActiveMatchRepository activeMatchRepository;
    private final ActiveMatchRemovalService activeMatchRemovalService;

    public FriendMatchDbCleanup(
            ActiveMatchRepository activeMatchRepository,
            ActiveMatchRemovalService activeMatchRemovalService
    ) {
        this.activeMatchRepository = activeMatchRepository;
        this.activeMatchRemovalService = activeMatchRemovalService;
    }

    @Override
    public void run(ApplicationArguments args) {
        var legacyIds = activeMatchRepository.findAllIdsByGameMode(GameModes.FRIEND);
        if (legacyIds.isEmpty()) {
            return;
        }
        log.info("Removing {} legacy friend active match(es) from database", legacyIds.size());
        for (String matchId : legacyIds) {
            activeMatchRemovalService.deleteByMatchId(matchId);
        }
    }
}
