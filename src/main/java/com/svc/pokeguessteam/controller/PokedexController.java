package com.svc.pokeguessteam.controller;

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
        List<Map<String, Object>> items = pokemonRepository.findAll()
                .stream()
                .map(pokemon -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("id", pokemon.getId());
                    payload.put("number", pokemon.getPokedexNumber());
                    payload.put("name", pokemon.getName());
                    payload.put("primaryType", pokemon.getPrimaryType());
                    payload.put("secondaryType", pokemon.getSecondaryType() == null ? "" : pokemon.getSecondaryType());
                    payload.put("generation", pokemon.getGeneration());
                    payload.put("color", pokemon.getColor());
                    payload.put("heightDm", pokemon.getHeightDm());
                    payload.put("weightHg", pokemon.getWeightHg());
                    payload.put("rarity", pokemon.getRarity().name());
                    return payload;
                })
                .toList();
        return ResponseEntity.ok(items);
    }
}
