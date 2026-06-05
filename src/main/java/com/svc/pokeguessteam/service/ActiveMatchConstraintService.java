package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Garante que um perfil só participa de uma partida não terminada de cada vez (qualquer modo).
 */
@Service
public class ActiveMatchConstraintService {

    private final ActiveMatchRepository activeMatchRepository;

    public ActiveMatchConstraintService(ActiveMatchRepository activeMatchRepository) {
        this.activeMatchRepository = activeMatchRepository;
    }

    public void ensureCanStartNewMatch(String profileId) {
        findUnfinishedMatch(profileId).ifPresent(ignored -> {
            throw new ApiBusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.GAME_MATCH_ALREADY_IN_PROGRESS,
                    MessageKeys.GAME_MATCH_ALREADY_IN_PROGRESS
            );
        });
    }

    public Optional<ActiveMatchModel> findUnfinishedMatch(String profileId) {
        return activeMatchRepository
                .findAllUnfinishedForProfileOrderByCreatedAtDesc(profileId, MatchStatus.FINISHED)
                .stream()
                .findFirst();
    }
}
