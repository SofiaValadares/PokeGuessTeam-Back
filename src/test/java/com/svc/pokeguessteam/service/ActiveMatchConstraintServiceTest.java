package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.repository.game.ActiveMatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveMatchConstraintServiceTest {

    @Mock
    private ActiveMatchRepository activeMatchRepository;

    @InjectMocks
    private ActiveMatchConstraintService service;

    @Test
    void blocksWhenUnfinishedMatchExistsInAnyMode() {
        ActiveMatchModel existing = new ActiveMatchModel();
        existing.setGameMode(GameModes.LOCAL);
        existing.setStatus(MatchStatus.SETUP);

        when(activeMatchRepository.findAllUnfinishedForProfileOrderByCreatedAtDesc(eq("profile-1"), eq(MatchStatus.FINISHED)))
                .thenReturn(List.of(existing));

        ApiBusinessException ex = assertThrows(
                ApiBusinessException.class,
                () -> service.ensureCanStartNewMatch("profile-1")
        );
        assertEquals(ErrorCodes.GAME_MATCH_ALREADY_IN_PROGRESS, ex.getCode());
    }

    @Test
    void allowsStartWhenNoUnfinishedMatch() {
        when(activeMatchRepository.findAllUnfinishedForProfileOrderByCreatedAtDesc(eq("profile-1"), eq(MatchStatus.FINISHED)))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.ensureCanStartNewMatch("profile-1"));
    }
}
