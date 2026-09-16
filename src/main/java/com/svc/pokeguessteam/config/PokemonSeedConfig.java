package com.svc.pokeguessteam.config;

import com.svc.pokeguessteam.config.seed.EvolutionLineSeedEntry;
import com.svc.pokeguessteam.config.seed.EvolutionLinesSeed;
import com.svc.pokeguessteam.config.seed.Generation1Seed;
import com.svc.pokeguessteam.config.seed.Generation2Seed;
import com.svc.pokeguessteam.config.seed.Generation3Seed;
import com.svc.pokeguessteam.config.seed.Generation4Seed;
import com.svc.pokeguessteam.config.seed.Generation5Seed;
import com.svc.pokeguessteam.config.seed.Generation6Seed;
import com.svc.pokeguessteam.config.seed.Generation7Seed;
import com.svc.pokeguessteam.config.seed.Generation8Seed;
import com.svc.pokeguessteam.config.seed.Generation9Seed;
import com.svc.pokeguessteam.config.seed.PokemonSeedEntry;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.repository.pokemon.EvolutionLineRepository;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seeds species + evolution lines on startup.
 * <p>
 * On Neon / pooled Postgres, per-row lookups for ~1000 species can keep a connection
 * open long enough for the proxy to close the socket and kill the whole boot.
 * This runner batches reads/writes and skips work when the catalog is already complete.
 */
@Component
public class PokemonSeedConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PokemonSeedConfig.class);
    private static final int MAX_ATTEMPTS = 3;

    private final PokemonRepository pokemonRepository;
    private final EvolutionLineRepository evolutionLineRepository;

    public PokemonSeedConfig(
            PokemonRepository pokemonRepository,
            EvolutionLineRepository evolutionLineRepository
    ) {
        this.pokemonRepository = pokemonRepository;
        this.evolutionLineRepository = evolutionLineRepository;
    }

    @Override
    public void run(String... args) {
        List<PokemonSeedEntry> entries = allPokemonEntries();
        int expectedPokemon = entries.size();
        int expectedLines = EvolutionLinesSeed.entries().size();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                long pokemonCount = pokemonRepository.count();
                long lineCount = evolutionLineRepository.count();
                if (pokemonCount >= expectedPokemon && lineCount >= expectedLines) {
                    log.info(
                            "Pokemon catalog already seeded ({} species, {} lines); skipping",
                            pokemonCount,
                            lineCount
                    );
                    return;
                }

                log.info(
                        "Seeding pokemon catalog (have {}/{} species, {}/{} lines)...",
                        pokemonCount,
                        expectedPokemon,
                        lineCount,
                        expectedLines
                );
                Map<Integer, EvolutionLineModel> linesByKey = seedEvolutionLines();
                Map<String, PokemonModel> byPokedexKey = seedPokemon(entries, linesByKey);
                applyEvolutionData(byPokedexKey);
                log.info("Pokemon catalog seed finished");
                return;
            } catch (DataAccessException ex) {
                log.warn(
                        "Pokemon seed attempt {}/{} failed: {}",
                        attempt,
                        MAX_ATTEMPTS,
                        ex.getMostSpecificCause() != null
                                ? ex.getMostSpecificCause().getMessage()
                                : ex.getMessage()
                );
                if (attempt == MAX_ATTEMPTS) {
                    try {
                        if (pokemonRepository.count() > 0) {
                            log.error(
                                    "Pokemon seed failed after {} attempts, but catalog has data — continuing startup",
                                    MAX_ATTEMPTS,
                                    ex
                            );
                            return;
                        }
                    } catch (DataAccessException countEx) {
                        log.error("Pokemon seed failed and catalog count is unreachable", countEx);
                    }
                    throw ex;
                }
                sleepBeforeRetry(attempt);
            }
        }
    }

    private Map<Integer, EvolutionLineModel> seedEvolutionLines() {
        List<EvolutionLineSeedEntry> seeds = EvolutionLinesSeed.entries();
        Map<Integer, EvolutionLineModel> existing = evolutionLineRepository.findAll().stream()
                .collect(Collectors.toMap(EvolutionLineModel::getLineKey, Function.identity(), (a, b) -> a));

        List<EvolutionLineModel> toInsert = new ArrayList<>();
        for (EvolutionLineSeedEntry seed : seeds) {
            if (!existing.containsKey(seed.lineKey())) {
                toInsert.add(seed.toModel());
            }
        }
        if (!toInsert.isEmpty()) {
            for (EvolutionLineModel saved : evolutionLineRepository.saveAll(toInsert)) {
                existing.put(saved.getLineKey(), saved);
            }
        }

        Map<Integer, EvolutionLineModel> byKey = new HashMap<>(existing.size());
        for (EvolutionLineSeedEntry seed : seeds) {
            EvolutionLineModel line = existing.get(seed.lineKey());
            if (line == null) {
                throw new IllegalStateException("Failed to seed evolution line key: " + seed.lineKey());
            }
            byKey.put(seed.lineKey(), line);
        }
        return byKey;
    }

    private Map<String, PokemonModel> seedPokemon(
            List<PokemonSeedEntry> entries,
            Map<Integer, EvolutionLineModel> linesByKey
    ) {
        Map<Integer, PokemonModel> existingByNumber = pokemonRepository.findAll().stream()
                .collect(Collectors.toMap(PokemonModel::getPokedexNumber, Function.identity(), (a, b) -> a));

        List<PokemonModel> toInsert = new ArrayList<>();
        Map<String, PokemonModel> byPokedexKey = new HashMap<>(entries.size());

        for (PokemonSeedEntry entry : entries) {
            String key = String.valueOf(entry.pokedexNumber());
            PokemonModel existing = existingByNumber.get(entry.pokedexNumber());
            if (existing != null) {
                byPokedexKey.put(key, existing);
                continue;
            }
            EvolutionLineModel line = linesByKey.get(entry.evolutionLineKey());
            if (line == null) {
                throw new IllegalStateException("Unknown evolution line key: " + entry.evolutionLineKey());
            }
            toInsert.add(entry.toModel(line));
        }

        if (!toInsert.isEmpty()) {
            for (PokemonModel saved : pokemonRepository.saveAll(toInsert)) {
                byPokedexKey.put(String.valueOf(saved.getPokedexNumber()), saved);
            }
        }
        return byPokedexKey;
    }

    private static List<PokemonSeedEntry> allPokemonEntries() {
        List<PokemonSeedEntry> all = new ArrayList<>();
        addGeneration(all, Generation1Seed.entries());
        addGeneration(all, Generation2Seed.entries());
        addGeneration(all, Generation3Seed.entries());
        addGeneration(all, Generation4Seed.entries());
        addGeneration(all, Generation5Seed.entries());
        addGeneration(all, Generation6Seed.entries());
        addGeneration(all, Generation7Seed.entries());
        addGeneration(all, Generation8Seed.entries());
        addGeneration(all, Generation9Seed.entries());
        return all;
    }

    private static void addGeneration(List<PokemonSeedEntry> target, List<PokemonSeedEntry> entries) {
        target.addAll(entries);
    }

    private void applyEvolutionData(Map<String, PokemonModel> byPokemonId) {
        Map<Integer, EvolutionStage> evolutionStage = new HashMap<>();
        Map<Integer, Integer> evolutionLevel = new HashMap<>();

        putEvolution(evolutionStage, evolutionLevel, 1, EvolutionStage.BASE, 16);
        putEvolution(evolutionStage, evolutionLevel, 2, EvolutionStage.FIRST_STAGE, 32);
        putEvolution(evolutionStage, evolutionLevel, 4, EvolutionStage.BASE, 16);
        putEvolution(evolutionStage, evolutionLevel, 5, EvolutionStage.FIRST_STAGE, 36);
        putEvolution(evolutionStage, evolutionLevel, 7, EvolutionStage.BASE, 16);
        putEvolution(evolutionStage, evolutionLevel, 8, EvolutionStage.FIRST_STAGE, 36);
        putEvolution(evolutionStage, evolutionLevel, 10, EvolutionStage.BASE, 7);
        putEvolution(evolutionStage, evolutionLevel, 11, EvolutionStage.FIRST_STAGE, 10);
        putEvolution(evolutionStage, evolutionLevel, 13, EvolutionStage.BASE, 7);
        putEvolution(evolutionStage, evolutionLevel, 14, EvolutionStage.FIRST_STAGE, 10);
        putEvolution(evolutionStage, evolutionLevel, 16, EvolutionStage.BASE, 18);
        putEvolution(evolutionStage, evolutionLevel, 17, EvolutionStage.FIRST_STAGE, 36);
        putEvolution(evolutionStage, evolutionLevel, 19, EvolutionStage.BASE, 20);
        putEvolution(evolutionStage, evolutionLevel, 21, EvolutionStage.BASE, 20);
        putEvolution(evolutionStage, evolutionLevel, 23, EvolutionStage.BASE, 22);
        putEvolution(evolutionStage, evolutionLevel, 25, EvolutionStage.BASE, null);
        putEvolution(evolutionStage, evolutionLevel, 27, EvolutionStage.BASE, 22);
        putEvolution(evolutionStage, evolutionLevel, 29, EvolutionStage.BASE, 16);
        putEvolution(evolutionStage, evolutionLevel, 30, EvolutionStage.FIRST_STAGE, null);
        putEvolution(evolutionStage, evolutionLevel, 32, EvolutionStage.BASE, 16);
        putEvolution(evolutionStage, evolutionLevel, 33, EvolutionStage.FIRST_STAGE, null);
        putEvolution(evolutionStage, evolutionLevel, 35, EvolutionStage.BASE, null);
        putEvolution(evolutionStage, evolutionLevel, 37, EvolutionStage.BASE, null);
        putEvolution(evolutionStage, evolutionLevel, 39, EvolutionStage.BASE, null);
        putEvolution(evolutionStage, evolutionLevel, 41, EvolutionStage.BASE, 22);
        putEvolution(evolutionStage, evolutionLevel, 43, EvolutionStage.BASE, 21);
        putEvolution(evolutionStage, evolutionLevel, 44, EvolutionStage.FIRST_STAGE, null);
        putEvolution(evolutionStage, evolutionLevel, 46, EvolutionStage.BASE, 24);
        putEvolution(evolutionStage, evolutionLevel, 48, EvolutionStage.BASE, 31);
        putEvolution(evolutionStage, evolutionLevel, 50, EvolutionStage.BASE, 26);
        putEvolution(evolutionStage, evolutionLevel, 52, EvolutionStage.BASE, 28);
        putEvolution(evolutionStage, evolutionLevel, 54, EvolutionStage.BASE, 33);
        putEvolution(evolutionStage, evolutionLevel, 56, EvolutionStage.BASE, 28);
        putEvolution(evolutionStage, evolutionLevel, 58, EvolutionStage.BASE, null);
        putEvolution(evolutionStage, evolutionLevel, 60, EvolutionStage.BASE, 25);
        putEvolution(evolutionStage, evolutionLevel, 61, EvolutionStage.FIRST_STAGE, null);
        putEvolution(evolutionStage, evolutionLevel, 63, EvolutionStage.BASE, 16);
        putEvolution(evolutionStage, evolutionLevel, 64, EvolutionStage.FIRST_STAGE, null);
        putEvolution(evolutionStage, evolutionLevel, 66, EvolutionStage.BASE, 28);
        putEvolution(evolutionStage, evolutionLevel, 67, EvolutionStage.FIRST_STAGE, null);
        putEvolution(evolutionStage, evolutionLevel, 69, EvolutionStage.BASE, 21);
        putEvolution(evolutionStage, evolutionLevel, 70, EvolutionStage.FIRST_STAGE, null);
        putEvolution(evolutionStage, evolutionLevel, 72, EvolutionStage.BASE, 30);
        putEvolution(evolutionStage, evolutionLevel, 74, EvolutionStage.BASE, 25);
        putEvolution(evolutionStage, evolutionLevel, 75, EvolutionStage.FIRST_STAGE, null);
        putEvolution(evolutionStage, evolutionLevel, 77, EvolutionStage.BASE, 40);
        putEvolution(evolutionStage, evolutionLevel, 79, EvolutionStage.BASE, 37);
        putEvolution(evolutionStage, evolutionLevel, 81, EvolutionStage.BASE, 30);
        putEvolution(evolutionStage, evolutionLevel, 84, EvolutionStage.BASE, 31);
        putEvolution(evolutionStage, evolutionLevel, 86, EvolutionStage.BASE, 34);
        putEvolution(evolutionStage, evolutionLevel, 88, EvolutionStage.BASE, 38);
        putEvolution(evolutionStage, evolutionLevel, 90, EvolutionStage.BASE, null);
        putEvolution(evolutionStage, evolutionLevel, 92, EvolutionStage.BASE, 25);
        putEvolution(evolutionStage, evolutionLevel, 93, EvolutionStage.FIRST_STAGE, null);
        putEvolution(evolutionStage, evolutionLevel, 96, EvolutionStage.BASE, 26);
        putEvolution(evolutionStage, evolutionLevel, 98, EvolutionStage.BASE, 28);
        putEvolution(evolutionStage, evolutionLevel, 100, EvolutionStage.BASE, 30);
        putEvolution(evolutionStage, evolutionLevel, 102, EvolutionStage.BASE, null);
        putEvolution(evolutionStage, evolutionLevel, 104, EvolutionStage.BASE, 28);
        putEvolution(evolutionStage, evolutionLevel, 109, EvolutionStage.BASE, 35);
        putEvolution(evolutionStage, evolutionLevel, 111, EvolutionStage.BASE, 42);
        putEvolution(evolutionStage, evolutionLevel, 116, EvolutionStage.BASE, 32);
        putEvolution(evolutionStage, evolutionLevel, 118, EvolutionStage.BASE, 33);
        putEvolution(evolutionStage, evolutionLevel, 120, EvolutionStage.BASE, null);
        putEvolution(evolutionStage, evolutionLevel, 129, EvolutionStage.BASE, 20);
        putEvolution(evolutionStage, evolutionLevel, 133, EvolutionStage.BASE, null);
        putEvolution(evolutionStage, evolutionLevel, 138, EvolutionStage.BASE, 40);
        putEvolution(evolutionStage, evolutionLevel, 140, EvolutionStage.BASE, 40);
        putEvolution(evolutionStage, evolutionLevel, 147, EvolutionStage.BASE, 30);
        putEvolution(evolutionStage, evolutionLevel, 148, EvolutionStage.FIRST_STAGE, 55);

        Set<PokemonModel> dirty = new HashSet<>();
        for (PokemonModel pokemon : byPokemonId.values()) {
            int number = pokemon.getPokedexNumber();
            if (evolutionStage.containsKey(number)) {
                pokemon.setEvolutionStage(evolutionStage.get(number));
            }
            if (evolutionLevel.containsKey(number)) {
                pokemon.setEvolutionLevel(evolutionLevel.get(number));
            }
            dirty.add(pokemon);
        }
        pokemonRepository.saveAll(dirty);
    }

    private static void putEvolution(
            Map<Integer, EvolutionStage> stageMap,
            Map<Integer, Integer> levelMap,
            int pokedexNumber,
            EvolutionStage stage,
            Integer level
    ) {
        stageMap.put(pokedexNumber, stage);
        levelMap.put(pokedexNumber, level);
    }

    private static void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
