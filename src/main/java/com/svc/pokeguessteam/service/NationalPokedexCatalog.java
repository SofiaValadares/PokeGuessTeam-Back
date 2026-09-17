package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.pokemon.EvolutionLineDto;
import com.svc.pokeguessteam.dto.pokemon.PokemonDto;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo nacional em memória — espécies mudam raramente; evita reler toda a TB_POKEMON a cada pedido.
 */
@Service
public class NationalPokedexCatalog {

    private final PokemonRepository pokemonRepository;
    private volatile CatalogSnapshot snapshot;

    public NationalPokedexCatalog(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    @Transactional(readOnly = true)
    public List<PokemonModel> allSpeciesOrdered() {
        return ensureSnapshot().species();
    }

    @Transactional(readOnly = true)
    public String pokedexVersion() {
        return ensureSnapshot().version();
    }

    @Transactional(readOnly = true)
    public List<PokemonDto> allSpeciesDtos() {
        return ensureSnapshot().speciesDtos();
    }

    @Transactional(readOnly = true)
    public List<EvolutionLineDto> allEvolutionLines() {
        return ensureSnapshot().evolutionLines();
    }

    private CatalogSnapshot ensureSnapshot() {
        CatalogSnapshot current = snapshot;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (snapshot == null) {
                List<PokemonModel> species = List.copyOf(pokemonRepository.findAllByOrderByPokedexNumberAsc());
                List<PokemonDto> speciesDtos = species.stream().map(PokemonDto::from).toList();
                List<EvolutionLineDto> lines = dedupeEvolutionLines(speciesDtos);
                String version = computeVersion(species);
                snapshot = new CatalogSnapshot(version, species, speciesDtos, lines);
            }
            return snapshot;
        }
    }

    private static List<EvolutionLineDto> dedupeEvolutionLines(List<PokemonDto> speciesDtos) {
        Map<Integer, EvolutionLineDto> byKey = new LinkedHashMap<>();
        for (PokemonDto species : speciesDtos) {
            EvolutionLineDto line = species.evolutionLine();
            if (line == null || line.key() == null) {
                continue;
            }
            byKey.putIfAbsent(line.key(), line);
        }
        return List.copyOf(byKey.values());
    }

    private static String computeVersion(List<PokemonModel> species) {
        StringBuilder canonical = new StringBuilder(species.size() * 8);
        canonical.append(species.size()).append('|');
        int maxDex = 0;
        for (PokemonModel pokemon : species) {
            int dex = pokemon.getPokedexNumber();
            if (dex > maxDex) {
                maxDex = dex;
            }
            canonical.append(dex).append(',');
        }
        canonical.append('|').append(maxDex);
        return sha256Hex(canonical.toString());
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private record CatalogSnapshot(
            String version,
            List<PokemonModel> species,
            List<PokemonDto> speciesDtos,
            List<EvolutionLineDto> evolutionLines
    ) {
    }
}
