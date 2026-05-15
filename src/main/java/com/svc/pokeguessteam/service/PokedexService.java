package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.pokemon.PokedexEntryDto;
import com.svc.pokeguessteam.dto.pokemon.PokedexEntryPageResponse;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class PokedexService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final PokemonRepository pokemonRepository;
    private final UserPokedexService userPokedexService;

    public PokedexService(PokemonRepository pokemonRepository, UserPokedexService userPokedexService) {
        this.pokemonRepository = pokemonRepository;
        this.userPokedexService = userPokedexService;
    }

    @Transactional(readOnly = true)
    public List<PokedexEntryDto> listAllForUser(String userId) {
        Set<Integer> registered = userPokedexService.findRegisteredPokedexNumbers(userId);
        return pokemonRepository.findAllByOrderByPokedexNumberAsc().stream()
                .map(p -> PokedexEntryDto.from(p, registered.contains(p.getPokedexNumber())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PokedexEntryPageResponse listPageForUser(String userId, int page, int size) {
        Set<Integer> registered = userPokedexService.findRegisteredPokedexNumbers(userId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "pokedexNumber")
        );
        Page<PokemonModel> speciesPage = pokemonRepository.findAll(pageable);
        List<PokedexEntryDto> content = speciesPage.getContent().stream()
                .map(p -> PokedexEntryDto.from(p, registered.contains(p.getPokedexNumber())))
                .toList();
        Page<PokedexEntryDto> entryPage = new PageImpl<>(
                content,
                pageable,
                speciesPage.getTotalElements()
        );
        return PokedexEntryPageResponse.from(entryPage);
    }
}
