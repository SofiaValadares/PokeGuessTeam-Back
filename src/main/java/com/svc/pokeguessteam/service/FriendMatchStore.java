package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.model.enums.MatchStatus;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Partidas amigo ativas apenas em memória — reinício do servidor apaga salas em curso.
 * O histórico concluído continua em {@code TB_GAME_HISTORY}.
 */
@Service
public class FriendMatchStore {

    private final ConcurrentHashMap<String, ActiveMatchModel> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> joinCodeToMatchId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> profileToMatchId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> matchLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> completionLocks = new ConcurrentHashMap<>();

    public void save(ActiveMatchModel match) {
        if (match == null || match.getId() == null) {
            throw new IllegalArgumentException("Partida amigo sem id.");
        }
        synchronized (matchLock(match.getId())) {
            normalizeGuesses(match);
            byId.put(match.getId(), match);
            if (match.getJoinCode() != null) {
                joinCodeToMatchId.put(match.getJoinCode(), match.getId());
            }
            profileToMatchId.put(match.getProfile().getId(), match.getId());
            if (match.getGuestProfile() != null) {
                profileToMatchId.put(match.getGuestProfile().getId(), match.getId());
            }
        }
    }

    public Optional<ActiveMatchModel> findById(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return Optional.empty();
        }
        synchronized (matchLock(matchId)) {
            return Optional.ofNullable(byId.get(matchId));
        }
    }

    public Optional<ActiveMatchModel> findByJoinCode(String joinCode) {
        if (joinCode == null || joinCode.isBlank()) {
            return Optional.empty();
        }
        String matchId = joinCodeToMatchId.get(joinCode);
        if (matchId == null) {
            return Optional.empty();
        }
        return findById(matchId);
    }

    public Optional<ActiveMatchModel> findActiveForProfile(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return Optional.empty();
        }
        String matchId = profileToMatchId.get(profileId);
        if (matchId == null) {
            return Optional.empty();
        }
        Optional<ActiveMatchModel> match = findById(matchId);
        if (match.isEmpty()) {
            profileToMatchId.remove(profileId, matchId);
        }
        return match;
    }

    public boolean exists(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return false;
        }
        synchronized (matchLock(matchId)) {
            return byId.containsKey(matchId);
        }
    }

    public boolean isJoinCodeTaken(String joinCode) {
        return joinCode != null && joinCodeToMatchId.containsKey(joinCode);
    }

    public void remove(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return;
        }
        synchronized (matchLock(matchId)) {
            ActiveMatchModel match = byId.remove(matchId);
            if (match == null) {
                return;
            }
            if (match.getJoinCode() != null) {
                joinCodeToMatchId.remove(match.getJoinCode(), matchId);
            }
            profileToMatchId.remove(match.getProfile().getId(), matchId);
            if (match.getGuestProfile() != null) {
                profileToMatchId.remove(match.getGuestProfile().getId(), matchId);
            }
        }
        matchLocks.remove(matchId);
    }

    public void removeForProfile(String profileId) {
        findActiveForProfile(profileId).ifPresent(match -> remove(match.getId()));
    }

    /**
     * Serializa leituras/escritas da mesma partida (palpite + timeout).
     */
    public <T> T callExclusive(String matchId, Supplier<T> action) {
        synchronized (matchLock(matchId)) {
            return action.get();
        }
    }

    public void runExclusive(String matchId, Runnable action) {
        synchronized (matchLock(matchId)) {
            action.run();
        }
    }

    /**
     * Garante que recompensas/remoção da partida correm uma única vez (ex.: desistência concorrente).
     */
    public <T> T completeOnce(String matchId, Supplier<T> action) {
        Object lock = completionLocks.computeIfAbsent(matchId, ignored -> new Object());
        synchronized (lock) {
            try {
                return action.get();
            } finally {
                completionLocks.remove(matchId, lock);
            }
        }
    }

    public static ActiveMatchModel newMatchShell() {
        ActiveMatchModel match = new ActiveMatchModel();
        match.setId(UUID.randomUUID().toString());
        match.setCreatedAt(LocalDateTime.now());
        match.setStatus(MatchStatus.SETUP);

        ActiveMatchPlayerModel hostPlayer = new ActiveMatchPlayerModel();
        hostPlayer.setId(UUID.randomUUID().toString());
        hostPlayer.setMatch(match);

        ActiveMatchPlayerModel guestPlayer = new ActiveMatchPlayerModel();
        guestPlayer.setId(UUID.randomUUID().toString());
        guestPlayer.setMatch(match);

        match.setHostPlayer(hostPlayer);
        match.setOpponentPlayer(guestPlayer);
        return match;
    }

    private static void normalizeGuesses(ActiveMatchModel match) {
        for (ActiveMatchGuessModel guess : List.copyOf(match.getGuesses())) {
            if (guess.getId() == null) {
                guess.setId(UUID.randomUUID().toString());
            }
            if (guess.getCreatedAt() == null) {
                guess.setCreatedAt(LocalDateTime.now());
            }
            guess.setMatch(match);
        }
    }

    private Object matchLock(String matchId) {
        return matchLocks.computeIfAbsent(matchId, ignored -> new Object());
    }
}
