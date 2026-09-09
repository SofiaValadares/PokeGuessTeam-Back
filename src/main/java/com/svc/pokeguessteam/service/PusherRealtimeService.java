package com.svc.pokeguessteam.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pusher.rest.Pusher;
import com.svc.pokeguessteam.config.AppPusherProperties;
import com.svc.pokeguessteam.dto.game.FriendMatchStateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fan-out realtime via Pusher Channels. Se não configurado, no-op (REST continua a funcionar).
 */
@Service
public class PusherRealtimeService {

    private static final Logger log = LoggerFactory.getLogger(PusherRealtimeService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    public static final String EVENT_MATCH_STATE = "match-state";
    public static final String EVENT_QUEUE_UPDATE = "queue-update";

    private final AppPusherProperties properties;
    private final ObjectMapper objectMapper;
    private final Pusher pusher;

    public PusherRealtimeService(AppPusherProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        if (properties.isConfigured()) {
            Pusher client = new Pusher(properties.getAppId(), properties.getKey(), properties.getSecret());
            client.setCluster(properties.getCluster());
            client.setEncrypted(true);
            this.pusher = client;
            log.info("Pusher realtime enabled (cluster={})", properties.getCluster());
        } else {
            this.pusher = null;
            log.info("Pusher realtime disabled (set APP_PUSHER_ENABLED=true and credentials)");
        }
    }

    public boolean isEnabled() {
        return pusher != null;
    }

    public String getPublicKey() {
        return properties.getKey();
    }

    public String getCluster() {
        return properties.getCluster();
    }

    public static String userChannel(String userId) {
        return "private-user-" + userId;
    }

    public static String matchChannel(String matchId) {
        return "private-match-" + matchId;
    }

    public Map<String, Object> authenticate(String socketId, String channelName) {
        if (pusher == null) {
            throw new IllegalStateException("Pusher não está configurado.");
        }
        String json = pusher.authenticate(socketId, channelName);
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of("auth", json);
        }
    }

    /**
     * Serializa via JSON Jackson (ISO dates) e volta a Map —
     * o cliente Pusher usa Gson e falha com {@link java.time.LocalDateTime} nativo.
     */
    private Object toPusherPayload(Object data) {
        if (data == null) {
            return Map.of();
        }
        try {
            String json = objectMapper.writeValueAsString(data);
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            log.warn("Falha a serializar payload Pusher: {}", ex.getMessage());
            return Map.of();
        }
    }

    public void publishUserEvent(String userId, String eventName, Object data) {
        if (pusher == null || userId == null || userId.isBlank()) {
            return;
        }
        try {
            pusher.trigger(userChannel(userId), eventName, toPusherPayload(data));
        } catch (Exception ex) {
            log.warn("Falha ao publicar Pusher user={} event={}: {}", userId, eventName, ex.getMessage());
        }
    }

    public void publishMatchState(String userId, FriendMatchStateDto state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "MATCH_STATE");
        payload.put("match", state);
        Object safePayload = toPusherPayload(payload);
        if (pusher == null || userId == null || userId.isBlank()) {
            return;
        }
        try {
            pusher.trigger(userChannel(userId), EVENT_MATCH_STATE, safePayload);
        } catch (Exception ex) {
            log.warn("Falha ao publicar Pusher user={} event={}: {}", userId, EVENT_MATCH_STATE, ex.getMessage());
        }
        if (state != null && state.matchId() != null) {
            try {
                pusher.trigger(matchChannel(state.matchId()), EVENT_MATCH_STATE, safePayload);
            } catch (Exception ex) {
                log.warn("Falha ao publicar Pusher match={}: {}", state.matchId(), ex.getMessage());
            }
        }
    }

    public void publishQueueWaiting(String userId, int registeredPokedexCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "WAITING");
        payload.put("registeredPokedexCount", registeredPokedexCount);
        publishUserEvent(userId, EVENT_QUEUE_UPDATE, payload);
    }

    public void publishQueueMatched(String userId, FriendMatchStateDto state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "MATCHED");
        payload.put("match", state);
        publishUserEvent(userId, EVENT_QUEUE_UPDATE, toPusherPayload(payload));
        publishMatchState(userId, state);
    }
}
