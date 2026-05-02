package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.model.EvolutionLineModel;
import com.svc.pokeguessteam.model.PokemonModel;
import com.svc.pokeguessteam.repository.PokemonRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pokedex")
public class PokedexController {

    private final PokemonRepository pokemonRepository;

    public PokedexController(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        List<PokemonModel> all = pokemonRepository.findAllByOrderByPokedexNumberAsc();
        List<Map<String, Object>> items = all.stream()
                .map(pokemon -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("id", String.valueOf(pokemon.getPokedexNumber()));
                    payload.put("number", pokemon.getPokedexNumber());
                    payload.put("name", pokemon.getName());
                    payload.put("primaryType", pokemon.getPrimaryType());
                    payload.put("secondaryType", pokemon.getSecondaryType() == null ? "" : pokemon.getSecondaryType());
                    payload.put("generation", pokemon.getGeneration());
                    payload.put("color", pokemon.getColor() == null ? "" : pokemon.getColor().name());
                    payload.put("heightM", pokemon.getHeightM());
                    payload.put("weightKg", pokemon.getWeightKg());
                    if (pokemon.getRarity() != null) {
                        payload.put("rarity", pokemon.getRarity().name());
                    }
                    payload.put("evolutionStage", pokemon.getEvolutionStage());
                    payload.put("evolutionLevel", pokemon.getEvolutionLevel());
                    EvolutionLineModel line = pokemon.getEvolutionLine();
                    if (line != null) {
                        Map<String, Object> linePayload = new HashMap<>();
                        linePayload.put("key", line.getLineKey());
                        linePayload.put("rarity", line.getRarity().name());
                        linePayload.put("members", List.copyOf(line.getMemberPokedexNumbers()));
                        payload.put("evolutionLine", linePayload);
                    }
                    return payload;
                })
                .toList();
        return ResponseEntity.ok(items);
    }
}
