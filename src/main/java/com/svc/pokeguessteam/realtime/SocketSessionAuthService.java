package com.svc.pokeguessteam.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
public class SocketSessionAuthService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SocketSessionAuthService(
            @Value("${server.port:8080}") int serverPort,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + serverPort)
                .build();
        this.objectMapper = objectMapper;
    }

    public Optional<String> resolveUserId(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return Optional.empty();
        }
        try {
            String body = restClient.get()
                    .uri("/auth/session")
                    .header("Cookie", cookieHeader)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode node = objectMapper.readTree(body);
            if (!node.path("authenticated").asBoolean(false)) {
                return Optional.empty();
            }
            JsonNode userIdNode = node.path("userId");
            if (userIdNode.isMissingNode() || userIdNode.isNull()) {
                return Optional.empty();
            }
            String userId = userIdNode.asText(null);
            if (userId == null || userId.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(userId);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
