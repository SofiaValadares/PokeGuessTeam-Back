package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.game.BotMatchTeamRequest;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.util.GameConstants;
import com.svc.pokeguessteam.util.MatchEngine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fila competitiva: emparelha jogadores com {@code registeredPokedexCount} o mais próximo possível.
 * A partida resultante vive no mesmo store que o modo amigo (ações via {@link FriendMatchService}).
 */
@Service
public class CompetitiveMatchService {

    private static final int[] MATCH_RADIUS = {5, 15, 40, Integer.MAX_VALUE};

    private final ProfileService profileService;
    private final DuelTeamService duelTeamService;
    private final ActiveMatchConstraintService activeMatchConstraintService;
    private final UserPokedexService userPokedexService;
    private final FriendMatchStore friendMatchStore;
    private final FriendMatchService friendMatchService;
    private final PusherRealtimeService pusherRealtimeService;

    private final ConcurrentHashMap<String, QueueEntry> queueByUserId = new ConcurrentHashMap<>();

    public CompetitiveMatchService(
            ProfileService profileService,
            DuelTeamService duelTeamService,
            ActiveMatchConstraintService activeMatchConstraintService,
            UserPokedexService userPokedexService,
            FriendMatchStore friendMatchStore,
            FriendMatchService friendMatchService,
            PusherRealtimeService pusherRealtimeService
    ) {
        this.profileService = profileService;
        this.duelTeamService = duelTeamService;
        this.activeMatchConstraintService = activeMatchConstraintService;
        this.userPokedexService = userPokedexService;
        this.friendMatchStore = friendMatchStore;
        this.friendMatchService = friendMatchService;
        this.pusherRealtimeService = pusherRealtimeService;
    }

    @Transactional
    public Map<String, Object> enqueue(String userId, BotMatchTeamRequest request) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        if (profile.getUser().isOnlineBannedNow()) {
            throw new ApiBusinessException(
                    HttpStatus.FORBIDDEN,
                    ErrorCodes.AUTH_USER_ONLINE_BANNED,
                    MessageKeys.AUTH_USER_ONLINE_BANNED
            );
        }
        activeMatchConstraintService.clearStaleClientSideMatches(profile.getId());
        activeMatchConstraintService.ensureCanStartNewMatch(profile.getId());

        Optional<FriendMatchStateDto> already = friendMatchService.findActiveMatch(userId);
        if (already.isPresent()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "MATCHED");
            body.put("match", already.get());
            return body;
        }

        List<Integer> team = duelTeamService.validateTeamFromRegisteredPokedex(userId, request.team(), null);
        int registered = (int) userPokedexService.countRegistered(profile.getId());

        QueueEntry self = new QueueEntry(userId, profile.getId(), team, registered, Instant.now());
        queueByUserId.put(userId, self);
        pusherRealtimeService.publishQueueWaiting(userId, registered);

        Optional<QueueEntry> opponent = findBestOpponent(self);
        if (opponent.isPresent()) {
            QueueEntry other = opponent.get();
            queueByUserId.remove(self.userId(), self);
            queueByUserId.remove(other.userId(), other);
            return createMatchedResponse(self, other);
        }

        Map<String, Object> waiting = new LinkedHashMap<>();
        waiting.put("status", "WAITING");
        waiting.put("registeredPokedexCount", registered);
        return waiting;
    }

    public Map<String, Object> queueStatus(String userId) {
        Optional<FriendMatchStateDto> active = friendMatchService.findActiveMatch(userId);
        if (active.isPresent()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "MATCHED");
            body.put("match", active.get());
            return body;
        }
        QueueEntry entry = queueByUserId.get(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        if (entry == null) {
            body.put("status", "IDLE");
            return body;
        }
        body.put("status", "WAITING");
        body.put("registeredPokedexCount", entry.registeredCount());
        return body;
    }

    public void leaveQueue(String userId) {
        queueByUserId.remove(userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "IDLE");
        pusherRealtimeService.publishUserEvent(userId, PusherRealtimeService.EVENT_QUEUE_UPDATE, payload);
    }

    private Optional<QueueEntry> findBestOpponent(QueueEntry self) {
        long waitedMs = Instant.now().toEpochMilli() - self.enqueuedAt().toEpochMilli();
        int radiusIndex = waitedMs < 5_000 ? 0 : waitedMs < 15_000 ? 1 : waitedMs < 30_000 ? 2 : 3;
        int radius = MATCH_RADIUS[radiusIndex];

        return queueByUserId.values().stream()
                .filter(other -> !other.userId().equals(self.userId()))
                .filter(other -> Math.abs(other.registeredCount() - self.registeredCount()) <= radius)
                .min(Comparator
                        .comparingInt((QueueEntry o) -> Math.abs(o.registeredCount() - self.registeredCount()))
                        .thenComparing(QueueEntry::enqueuedAt));
    }

    private Map<String, Object> createMatchedResponse(QueueEntry a, QueueEntry b) {
        // Anfitrião = quem entrou primeiro
        QueueEntry host = a.enqueuedAt().isBefore(b.enqueuedAt()) ? a : b;
        QueueEntry guest = host == a ? b : a;

        ProfileModel hostProfile = profileService.ensureProfileWithStarters(host.userId());
        ProfileModel guestProfile = profileService.ensureProfileWithStarters(guest.userId());

        ActiveMatchModel match = FriendMatchStore.newMatchShell();
        match.setProfile(hostProfile);
        match.setGuestProfile(guestProfile);
        match.setGameMode(GameModes.COMPETITIVE);
        match.setJoinCode(null);
        match.getHostPlayer().setSide(MatchPlayerSide.HOST);
        match.getHostPlayer().setTeam(host.team());
        match.getOpponentPlayer().setSide(MatchPlayerSide.OPPONENT);
        match.getOpponentPlayer().setTeam(guest.team());
        MatchEngine.tryStartIfBothTeamsReady(match, GameConstants.TEAM_SIZE);
        friendMatchStore.save(match);

        FriendMatchStateDto hostState = friendMatchService.getStateForUser(match.getId(), host.userId());
        FriendMatchStateDto guestState = friendMatchService.getStateForUser(match.getId(), guest.userId());
        pusherRealtimeService.publishQueueMatched(host.userId(), hostState);
        pusherRealtimeService.publishQueueMatched(guest.userId(), guestState);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "MATCHED");
        // O caller recebe a vista do próprio user — o enqueue devolve a vista de `a` se for a, etc.
        // Aqui devolvemos estado genérico do host; o controller re-seleciona pelo userId.
        body.put("matchHost", hostState);
        body.put("matchGuest", guestState);
        body.put("hostUserId", host.userId());
        body.put("guestUserId", guest.userId());
        return body;
    }

    private record QueueEntry(
            String userId,
            String profileId,
            List<Integer> team,
            int registeredCount,
            Instant enqueuedAt
    ) {
    }
}
