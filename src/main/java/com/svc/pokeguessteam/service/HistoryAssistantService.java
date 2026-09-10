package com.svc.pokeguessteam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.svc.pokeguessteam.config.AppAiProperties;
import com.svc.pokeguessteam.dto.ai.HistoryChatResponse;
import com.svc.pokeguessteam.dto.ai.HistorySummaryDto;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class HistoryAssistantService {

    private static final DateTimeFormatter HISTORY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final HistoryAnalyticsService historyAnalyticsService;
    private final AppAiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public HistoryAssistantService(
            HistoryAnalyticsService historyAnalyticsService,
            AppAiProperties aiProperties,
            ObjectMapper objectMapper
    ) {
        this.historyAnalyticsService = historyAnalyticsService;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public HistoryChatResponse chat(String userId, String message) {
        HistorySummaryDto summary = historyAnalyticsService.buildSummary(userId);
        String normalizedMessage = normalize(message);
        String intent = detectIntent(normalizedMessage);

        if (aiProperties.isConfigured()) {
            String aiAnswer = generateAiAnswer(message, summary);
            if (StringUtils.hasText(aiAnswer)) {
                return new HistoryChatResponse(aiAnswer, intent, true, summary);
            }
        }

        return new HistoryChatResponse(composeFallbackAnswer(intent, summary), intent, false, summary);
    }

    private String generateAiAnswer(String message, HistorySummaryDto summary) {
        try {
            String summaryJson = objectMapper.writeValueAsString(summary);
            ChatCompletionRequest request = new ChatCompletionRequest(
                    aiProperties.getModel(),
                    List.of(
                            new ChatMessage(
                                    "system",
                                    "Você é um assistente do histórico de partidas do PokeGuessTeam. Responda em português europeu, com frases curtas e objetivas. Use apenas os dados fornecidos. Nunca invente números. Se um dado não estiver disponível, diga isso claramente."
                            ),
                            new ChatMessage(
                                    "user",
                                    "Pergunta do utilizador: " + message + "\n\nDados do histórico em JSON:\n" + summaryJson
                            )
                    ),
                    0.2
            );

            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(aiProperties.getApiKey()))
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return null;
            }

            ChatCompletionChoice choice = response.choices().get(0);
            if (choice.message() == null || !StringUtils.hasText(choice.message().content())) {
                return null;
            }
            return choice.message().content().trim();
        } catch (JsonProcessingException ex) {
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String composeFallbackAnswer(String intent, HistorySummaryDto summary) {
        if (summary.totalMatches() == 0) {
            return "Ainda não tens partidas registadas no histórico.";
        }

        if ("pokemon_usage".equals(intent)) {
            if (summary.mostUsedPokemonName() == null) {
                return "Ainda não consigo identificar o Pokémon mais usado porque as equipas antigas não foram guardadas no histórico.";
            }
            return String.format(
                    Locale.ROOT,
                    "O teu Pokémon mais usado é %s (#%d), com %d ocorrências no histórico.",
                    summary.mostUsedPokemonName(),
                    summary.mostUsedPokemonDex(),
                    summary.mostUsedPokemonCount()
            );
        }

        if ("recent_matches".equals(intent)) {
            return buildRecentMatchesAnswer(summary);
        }

        String bestModeLabel = summary.bestMode() != null ? summary.bestMode().name() : "sem dados suficientes";
        return String.format(
                Locale.ROOT,
                "Tens %d partidas registadas, com %d vitórias, %d derrotas, %d empates e %d desistências. A tua taxa de vitória é de %.1f%%. O modo com melhor desempenho atual é %s. A média de acertos por partida é %.1f.",
                summary.totalMatches(),
                summary.wins(),
                summary.losses(),
                summary.draws(),
                summary.desistences(),
                summary.winRate(),
                bestModeLabel,
                summary.averageCorrectGuesses()
        );
    }

    private static String buildRecentMatchesAnswer(HistorySummaryDto summary) {
        StringBuilder builder = new StringBuilder("Últimas partidas:\n");
        for (GameHistoryEntryDto game : summary.recentMatches()) {
            builder.append("- ")
                    .append(game.playedAt().format(HISTORY_DATE_FORMAT))
                    .append(" | ")
                    .append(game.gameMode())
                    .append(" | ")
                    .append(game.players().isEmpty() ? "sem jogadores" : game.players().get(0).result())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private static String detectIntent(String message) {
        if (containsAny(message, "pokemon", "pokémon", "mais escolhido", "mais usei", "mais usado", "time", "equipa")) {
            return "pokemon_usage";
        }
        if (containsAny(message, "últimas", "ultimas", "recent", "recentes")) {
            return "recent_matches";
        }
        if (containsAny(message, "taxa", "percentagem", "win rate", "porcentagem")) {
            return "win_rate";
        }
        if (containsAny(message, "vitória", "vitoria", "ganhei", "venci", "wins")) {
            return "wins";
        }
        if (containsAny(message, "derrota", "perdi", "loss")) {
            return "losses";
        }
        if (containsAny(message, "empate", "draw")) {
            return "draws";
        }
        return "summary";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }

    private record ChatCompletionRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatCompletionResponse(List<ChatCompletionChoice> choices) {
    }

    private record ChatCompletionChoice(ChatCompletionMessage message) {
    }

    private record ChatCompletionMessage(String content) {
    }
}