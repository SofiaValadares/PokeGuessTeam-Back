package com.svc.pokeguessteam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.svc.pokeguessteam.config.AppAiProperties;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class GameAnomalyAiServiceTest {

    @Test
    void usesConfiguredAiProviderAndStoresValidatedResult() {
        AppAiProperties properties = configuredOpenAi();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"{\\"players\\":[{\\"side\\":\\"HOST\\",\\"score\\":15,\\"reasons\\":[]},{\\"side\\":\\"OPPONENT\\",\\"score\\":72,\\"reasons\\":[\\"Sequência automatizada provável\\"]}]}"}}]}
                        """, MediaType.APPLICATION_JSON));

        GameAnomalyAiService service = new GameAnomalyAiService(properties, new ObjectMapper(), builder, false);
        GameAnomalyAiService.Analysis result = service.analyze(matchWithGuesses());

        assertEquals("ANALYZED", result.status());
        assertEquals(72, result.score());
        assertTrue(service.shouldCancelMatch(result));
        assertEquals(1, result.reasons().size());
        assertTrue(result.reasons().get(0).startsWith("OPPONENT:"));
        server.verify();
    }

    @Test
    void marksAnalysisUnavailableWhenNoAiKeyIsConfigured() {
        AppAiProperties properties = new AppAiProperties();
        GameAnomalyAiService service = new GameAnomalyAiService(
                properties,
                new ObjectMapper(),
                RestClient.builder()
        );

        GameAnomalyAiService.Analysis result = service.analyze(matchWithGuesses());

        assertEquals("UNAVAILABLE", result.status());
        assertEquals(0, result.score());
        assertTrue(!service.shouldCancelMatch(result));
        assertEquals("AI_NOT_CONFIGURED", result.reasons().get(0));
    }

    @Test
    void supportsConfiguredGeminiProvider() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("gemini");
        properties.setApiKey("test-key");
        properties.setModel("gemini-2.5-flash");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=test-key"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"players\\":[{\\"side\\":\\"HOST\\",\\"score\\":8,\\"reasons\\":[]},{\\"side\\":\\"OPPONENT\\",\\"score\\":0,\\"reasons\\":[]}] }"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        GameAnomalyAiService service = new GameAnomalyAiService(properties, new ObjectMapper(), builder, false);
        GameAnomalyAiService.Analysis result = service.analyze(matchWithGuesses());

        assertEquals("ANALYZED", result.status());
        assertEquals(8, result.score());
        server.verify();
    }

    @Test
    void rejectsMalformedAiOutputInsteadOfTreatingItAsClean() {
        AppAiProperties properties = configuredOpenAi();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"not-json\"}}]}", MediaType.APPLICATION_JSON));

        GameAnomalyAiService service = new GameAnomalyAiService(properties, new ObjectMapper(), builder, false);
        GameAnomalyAiService.Analysis result = service.analyze(matchWithGuesses());

        assertEquals("UNAVAILABLE", result.status());
        assertEquals("AI_ANALYSIS_FAILED", result.reasons().get(0));
        server.verify();
    }

    private static AppAiProperties configuredOpenAi() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("openai");
        properties.setApiKey("test-key");
        properties.setModel("gpt-4o-mini");
        return properties;
    }

    private static ActiveMatchModel matchWithGuesses() {
        ActiveMatchModel match = new ActiveMatchModel();
        addGuess(match, MatchPlayerSide.HOST, true, 0);
        addGuess(match, MatchPlayerSide.OPPONENT, false, 1_500);
        return match;
    }

    private static void addGuess(
            ActiveMatchModel match,
            MatchPlayerSide side,
            boolean exact,
            long millisAfterStart
    ) {
        ActiveMatchGuessModel guess = new ActiveMatchGuessModel();
        guess.setPlayerSide(side);
        guess.setExactMatch(exact);
        guess.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0).plusNanos(millisAfterStart * 1_000_000));
        match.addGuess(guess);
    }
}