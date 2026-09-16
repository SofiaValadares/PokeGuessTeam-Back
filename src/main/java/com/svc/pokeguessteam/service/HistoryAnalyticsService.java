package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.ai.HistoryModeSummaryDto;
import com.svc.pokeguessteam.dto.ai.HistorySummaryDto;
import com.svc.pokeguessteam.dto.game.GameHistoryEntryDto;
import com.svc.pokeguessteam.model.enums.GameModes;
import com.svc.pokeguessteam.model.enums.GameResults;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.game.HistoryGameModel;
import com.svc.pokeguessteam.model.game.HistoryGamePlayerModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.repository.game.HistoryGameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HistoryAnalyticsService {

    private static final int RECENT_MATCHES_LIMIT = 10;

    private final HistoryGameRepository historyGameRepository;
    private final ProfileService profileService;
        private final PokemonRepository pokemonRepository;

        public HistoryAnalyticsService(
                        HistoryGameRepository historyGameRepository,
                        ProfileService profileService,
                        PokemonRepository pokemonRepository
        ) {
        this.historyGameRepository = historyGameRepository;
        this.profileService = profileService;
                this.pokemonRepository = pokemonRepository;
    }

    @Transactional(readOnly = true)
    public HistorySummaryDto buildSummary(String userId) {
        ProfileModel profile = profileService.ensureProfileWithStarters(userId);
        List<HistoryGameModel> games = historyGameRepository.findAllByProfileIdOrderByPlayedAtDesc(profile.getId());

        int totalMatches = games.size();
        int wins = countByResult(games, profile.getId(), GameResults.WIN);
        int losses = countByResult(games, profile.getId(), GameResults.LOSE);
        int draws = countByResult(games, profile.getId(), GameResults.DRAW);
        int desistences = countByResult(games, profile.getId(), GameResults.DESISTENCE);
        double winRate = totalMatches == 0 ? 0.0 : (wins * 100.0 / totalMatches);
        double averageCorrectGuesses = totalMatches == 0
                ? 0.0
                : games.stream()
                        .mapToInt(game -> findUserPlayer(game, profile.getId())
                                .map(HistoryGamePlayerModel::getCorrectGuesses)
                                .orElse(0))
                        .average()
                        .orElse(0.0);

        TopPokemonUsage topPokemonUsage = findTopPokemonUsage(games, profile.getId());

        List<HistoryModeSummaryDto> modeSummaries = java.util.Arrays.stream(GameModes.values())
                .map(mode -> summarizeMode(games, profile.getId(), mode))
                .toList();

        GameModes bestMode = modeSummaries.stream()
                .filter(summary -> summary.matches() > 0)
                .max(Comparator.comparingDouble(HistoryModeSummaryDto::winRate)
                        .thenComparingInt(HistoryModeSummaryDto::matches))
                .map(HistoryModeSummaryDto::gameMode)
                .orElse(null);

        List<GameHistoryEntryDto> recentMatches = games.stream()
                .limit(RECENT_MATCHES_LIMIT)
                .map(GameHistoryEntryDto::from)
                .toList();

        return new HistorySummaryDto(
                totalMatches,
                wins,
                losses,
                draws,
                desistences,
                winRate,
                averageCorrectGuesses,
                topPokemonUsage.name(),
                topPokemonUsage.dexNumber(),
                topPokemonUsage.count(),
                bestMode,
                modeSummaries,
                recentMatches
        );
    }

    private TopPokemonUsage findTopPokemonUsage(List<HistoryGameModel> games, String profileId) {
        Map<Integer, Integer> usageCounts = new HashMap<>();
        for (HistoryGameModel game : games) {
            findUserPlayer(game, profileId)
                    .map(HistoryGamePlayerModel::getSelectedTeam)
                    .map(HistoryAnalyticsService::decodeTeam)
                    .orElse(List.of())
                    .forEach(dex -> usageCounts.merge(dex, 1, Integer::sum));
        }

        if (usageCounts.isEmpty()) {
            return new TopPokemonUsage(null, null, 0);
        }

        int topDex = usageCounts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
        int count = usageCounts.getOrDefault(topDex, 0);
        String name = pokemonRepository.findByPokedexNumber(topDex)
                .map(PokemonModel::getName)
                .orElse("#" + topDex);
                return new TopPokemonUsage(name, topDex, count);
    }

    private static int countByResult(List<HistoryGameModel> games, String profileId, GameResults result) {
        return (int) games.stream()
                .map(game -> findUserPlayer(game, profileId))
                .flatMap(Optional::stream)
                .filter(player -> player.getResult() == result)
                .count();
    }

    private static HistoryModeSummaryDto summarizeMode(List<HistoryGameModel> games, String profileId, GameModes mode) {
        List<HistoryGameModel> filtered = games.stream()
                .filter(game -> game.getGameMode() == mode)
                .toList();
        int matches = filtered.size();
        int wins = countByResult(filtered, profileId, GameResults.WIN);
        int losses = countByResult(filtered, profileId, GameResults.LOSE);
        int draws = countByResult(filtered, profileId, GameResults.DRAW);
        int desistences = countByResult(filtered, profileId, GameResults.DESISTENCE);
        double winRate = matches == 0 ? 0.0 : (wins * 100.0 / matches);
        return new HistoryModeSummaryDto(mode, matches, wins, losses, draws, desistences, winRate);
    }

    private static Optional<HistoryGamePlayerModel> findUserPlayer(HistoryGameModel game, String profileId) {
        return game.getPlayers().stream()
                .filter(player -> player.getProfile() != null && profileId.equals(player.getProfile().getId()))
                .findFirst();
    }

        private static List<Integer> decodeTeam(String encodedTeam) {
                if (encodedTeam == null || encodedTeam.isBlank()) {
                        return List.of();
        }
                String[] parts = encodedTeam.split(",");
                List<Integer> values = new ArrayList<>(parts.length);
                for (String part : parts) {
                        try {
                                values.add(Integer.parseInt(part.trim()));
                        } catch (NumberFormatException ignored) {
                                // Skip malformed entries.
                        }
                }
                return values;
        }

        private record TopPokemonUsage(String name, Integer dexNumber, int count) {
        }
}