package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.pokemon.PokedexPageResponse;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pokedex")
public class PokedexController {

    static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final PokemonRepository pokemonRepository;

    public PokedexController(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    /**
     * @param page índice baseado em zero (primeira página = {@code 0})
     * @param size quantidade por página (padrão {@value #DEFAULT_PAGE_SIZE}, máximo 100)
     */
    @GetMapping
    public ResponseEntity<PokedexPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "pokedexNumber"));
        Page<PokemonModel> result = pokemonRepository.findAll(pageable);
        return ResponseEntity.ok(PokedexPageResponse.from(result));
    }
}
