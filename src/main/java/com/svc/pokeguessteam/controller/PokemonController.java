package com.svc.pokeguessteam.controller;

import com.svc.pokeguessteam.dto.pokemon.PcPageResponse;
import com.svc.pokeguessteam.dto.pokemon.PokeballDrawRequest;
import com.svc.pokeguessteam.dto.pokemon.PokeballDrawResponse;
import com.svc.pokeguessteam.dto.pokemon.PokemonDto;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.service.CurrentUserService;
import com.svc.pokeguessteam.service.PokeballDrawService;
import com.svc.pokeguessteam.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pokemon")
public class PokemonController {

    static final int PC_DEFAULT_PAGE_SIZE = ProfileService.PokemonPcConstants.DEFAULT_PAGE_SIZE;
    private static final int PC_MAX_PAGE_SIZE = ProfileService.PokemonPcConstants.MAX_PAGE_SIZE;

    private final ProfileService profileService;
    private final PokemonRepository pokemonRepository;
    private final CurrentUserService currentUserService;
    private final PokeballDrawService pokeballDrawService;

    public PokemonController(
            ProfileService profileService,
            PokemonRepository pokemonRepository,
            CurrentUserService currentUserService,
            PokeballDrawService pokeballDrawService
    ) {
        this.profileService = profileService;
        this.pokemonRepository = pokemonRepository;
        this.currentUserService = currentUserService;
        this.pokeballDrawService = pokeballDrawService;
    }

    /**
     * PC do jogador: inventário por linha evolutiva (capturas / XP), paginado (default 20, máx. 100).
     */
    @GetMapping("/pc")
    public ResponseEntity<PcPageResponse> pc(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PC_DEFAULT_PAGE_SIZE) int size
    ) {
        String userId = currentUserService.requireUserId(session);
        profileService.ensureProfileWithStarters(userId);
        int safeSize = Math.min(Math.max(size, 1), PC_MAX_PAGE_SIZE);
        return ResponseEntity.ok(profileService.getPokemonPcPage(userId, page, safeSize));
    }

    /**
     * Consome uma Pokébola do inventário, sorteia raridade conforme o tipo e adiciona ao PC (linha evolutiva).
     */
    @PostMapping("/draw")
    public ResponseEntity<PokeballDrawResponse> draw(
            HttpSession session,
            @Valid @RequestBody PokeballDrawRequest request
    ) {
        String userId = currentUserService.requireUserId(session);
        return ResponseEntity.ok(pokeballDrawService.draw(userId, request.pokeballType()));
    }

    @GetMapping("/species/{pokedexNumber}")
    public ResponseEntity<PokemonDto> species(@PathVariable int pokedexNumber) {
        return pokemonRepository.findByPokedexNumber(pokedexNumber)
                .map(PokemonDto::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.POKEMON_SPECIES_NOT_FOUND,
                        MessageKeys.POKEMON_SPECIES_NOT_FOUND
                ));
    }

    /**
     * Pesquisa por nome para o campo de palpite (autocomplete na partida).
     */
    @GetMapping("/search")
    public ResponseEntity<List<PokemonDto>> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "30") int limit
    ) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<PokemonDto> results = pokemonRepository
                .findByNameContainingIgnoreCaseOrderByPokedexNumberAsc(trimmed, PageRequest.of(0, safeLimit))
                .stream()
                .map(PokemonDto::from)
                .toList();
        return ResponseEntity.ok(results);
    }
}
