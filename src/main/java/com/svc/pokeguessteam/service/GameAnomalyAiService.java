package com.svc.pokeguessteam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.svc.pokeguessteam.config.AppAiProperties;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GameAnomalyAiService {

    private static final String SYSTEM_PROMPT = """
            Você analisa partidas do jogo PokeGuessTeam para identificar evidências de automação ou comportamento anormal.
            Os eventos e tempos foram medidos pelo servidor. Não considere habilidade alta ou muitos acertos, isoladamente, como fraude.
            Seja conservador, considere o tamanho da amostra e retorne somente JSON válido no formato:
            {"players":[{"side":"HOST","score":0,"reasons":[]},{"side":"OPPONENT","score":0,"reasons":[]}]}
            Cada lado deve aparecer uma vez. score é um inteiro de 0 a 100 que representa evidência de comportamento anormal.
            Inclua no máximo três motivos curtos por jogador. Sem evidências, use score 0 e lista vazia.
            """;

    private final AppAiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Autowired
    public GameAnomalyAiService(
            AppAiProperties aiProperties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this(aiProperties, objectMapper, restClientBuilder, true);
    }

    GameAnomalyAiService(
            AppAiProperties aiProperties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            boolean boundedRequests
    ) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restClientBuilder = restClientBuilder.clone();
        if (boundedRequests) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(2_000);
            requestFactory.setReadTimeout(5_000);
            this.restClientBuilder.requestFactory(requestFactory);
        }
    }

    public Analysis analyze(ActiveMatchModel match) {
        if (!aiProperties.isConfigured()) {
            return Analysis.unavailable("AI_NOT_CONFIGURED");
        }

        try {
            String prompt = "Analise os sinais desta partida. Os tempos estão em milissegundos.\n"
                    + objectMapper.writeValueAsString(buildSignals(match));
            String response = aiProperties.usesGemini()
                    ? generateGeminiResponse(prompt)
                    : generateOpenAiResponse(prompt);
            if (!StringUtils.hasText(response)) {
                return Analysis.unavailable("AI_EMPTY_RESPONSE");
            }
            return parseResponse(response);
        } catch (RuntimeException | JsonProcessingException ex) {
            return Analysis.unavailable("AI_ANALYSIS_FAILED");
        }
    }

    public boolean shouldCancelMatch(Analysis analysis) {
        return analysis != null
                && "ANALYZED".equals(analysis.status())
                && analysis.score() >= aiProperties.getAnomalyCancelThreshold();
    }

    Analysis parseResponse(String response) throws JsonProcessingException {
        JsonNode players = objectMapper.readTree(response).path("players");
        if (!players.isArray() || players.size() != 2) {
            throw new IllegalArgumentException("Resposta de análise inválida.");
        }

        Set<String> seenSides = new HashSet<>();
        List<String> reasons = new ArrayList<>();
        int maxScore = 0;
        for (JsonNode player : players) {
            String side = player.path("side").asText("");
            JsonNode scoreNode = player.path("score");
            JsonNode reasonNodes = player.path("reasons");
            if ((!"HOST".equals(side) && !"OPPONENT".equals(side))
                    || !seenSides.add(side)
                    || !scoreNode.canConvertToInt()
                    || !reasonNodes.isArray()
                    || reasonNodes.size() > 3) {
                throw new IllegalArgumentException("Resposta de análise inválida.");
            }

            int score = scoreNode.asInt();
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("Pontuação de análise fora do intervalo.");
            }
            maxScore = Math.max(maxScore, score);

            for (JsonNode reasonNode : reasonNodes) {
                String reason = reasonNode.asText("").trim();
                if (!StringUtils.hasText(reason) || reason.length() > 120) {
                    throw new IllegalArgumentException("Motivo de análise inválido.");
                }
                reasons.add(side + ":" + reason);
            }
        }
        if (!seenSides.containsAll(Set.of("HOST", "OPPONENT"))) {
            throw new IllegalArgumentException("A resposta deve avaliar os dois jogadores.");
        }
        return new Analysis(maxScore, List.copyOf(reasons), "ANALYZED");
    }

    private List<GuessSignal> buildSignals(ActiveMatchModel match) {
        List<ActiveMatchGuessModel> guesses = match.getGuesses().stream()
                .filter(guess -> guess.getCreatedAt() != null)
                .sorted(Comparator.comparing(ActiveMatchGuessModel::getCreatedAt))
                .toList();
        List<GuessSignal> signals = new ArrayList<>(guesses.size());
        LocalDateTime previousAt = null;

        for (ActiveMatchGuessModel guess : guesses) {
            Long elapsedMillis = previousAt == null
                    ? null
                    : Math.max(0, Duration.between(previousAt, guess.getCreatedAt()).toMillis());
            signals.add(new GuessSignal(
                    guess.getPlayerSide().name(),
                    elapsedMillis,
                    guess.isExactMatch(),
                    guess.isTimedOut(),
                    guess.isAutoSelected()
            ));
            previousAt = guess.getCreatedAt();
        }
        return signals;
    }

    private String generateOpenAiResponse(String prompt) throws JsonProcessingException {
        ChatCompletionRequest request = new ChatCompletionRequest(
                aiProperties.getModel(),
                List.of(
                        new ChatMessage("system", SYSTEM_PROMPT),
                        new ChatMessage("user", prompt)
                ),
                0,
                new ResponseFormat("json_object")
        );
        ChatCompletionResponse response = restClientBuilder.clone()
                .baseUrl(aiProperties.getBaseUrl())
                .build()
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(aiProperties.getApiKey()))
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null) {
            return null;
        }
        return response.choices().get(0).message().content();
    }

    private String generateGeminiResponse(String prompt) throws JsonProcessingException {
        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                new GeminiSystemInstruction(List.of(new GeminiPart(SYSTEM_PROMPT))),
                List.of(new GeminiContent("user", List.of(new GeminiPart(prompt)))),
                new GeminiGenerationConfig(0, "application/json")
        );
        GeminiGenerateContentResponse response = restClientBuilder.clone()
                .baseUrl(aiProperties.resolveGeminiBaseUrl())
                .build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", aiProperties.getApiKey())
                        .build(aiProperties.resolveGeminiModel()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiGenerateContentResponse.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()
                || response.candidates().get(0).content() == null
                || response.candidates().get(0).content().parts() == null) {
            return null;
        }
        return response.candidates().get(0).content().parts().stream()
                .map(GeminiPart::text)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    public record Analysis(int score, List<String> reasons, String status) {
        public Analysis {
            reasons = List.copyOf(reasons);
        }

        static Analysis unavailable(String reason) {
            return new Analysis(0, List.of(reason), "UNAVAILABLE");
        }
    }

    private record GuessSignal(
            String side,
            Long elapsedSincePreviousGuessMillis,
            boolean exactMatch,
            boolean timedOut,
            boolean autoSelected
    ) {
    }

    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            double temperature,
            @JsonProperty("response_format") ResponseFormat responseFormat
    ) {
    }

    private record ResponseFormat(String type) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatCompletionResponse(List<ChatCompletionChoice> choices) {
    }

    private record ChatCompletionChoice(ChatCompletionMessage message) {
    }

    private record ChatCompletionMessage(String content) {
    }

    private record GeminiGenerateContentRequest(
            GeminiSystemInstruction systemInstruction,
            List<GeminiContent> contents,
            GeminiGenerationConfig generationConfig
    ) {
    }

    private record GeminiSystemInstruction(List<GeminiPart> parts) {
    }

    private record GeminiContent(String role, List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }

    private record GeminiGenerationConfig(double temperature, String responseMimeType) {
    }

    private record GeminiGenerateContentResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }
}