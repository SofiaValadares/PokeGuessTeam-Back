package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.dto.pokemon.PokedexEntryDto;
import com.svc.pokeguessteam.dto.pokemon.PokedexEntryPageResponse;
import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class PokedexService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final NationalPokedexCatalog nationalPokedexCatalog;
    private final PokemonRepository pokemonRepository;
    private final ProfileRepository profileRepository;
    private final UserPokedexService userPokedexService;

    public PokedexService(
            NationalPokedexCatalog nationalPokedexCatalog,
            PokemonRepository pokemonRepository,
            ProfileRepository profileRepository,
            UserPokedexService userPokedexService
    ) {
        this.nationalPokedexCatalog = nationalPokedexCatalog;
        this.pokemonRepository = pokemonRepository;
        this.profileRepository = profileRepository;
        this.userPokedexService = userPokedexService;
    }

    @Transactional
    public List<PokedexEntryDto> listRegisteredForUser(String userId) {
        ProfileModel profile = requireProfile(userId);
        userPokedexService.syncFromOwnership(profile);
        Set<Integer> registered = userPokedexService.findRegisteredPokedexNumbers(userId);
        if (registered.isEmpty()) {
            return List.of();
        }
        return pokemonRepository.findByPokedexNumberIn(registered).stream()
                .sorted(Comparator.comparingInt(PokemonModel::getPokedexNumber))
                .map(p -> PokedexEntryDto.from(p, true))
                .toList();
    }

    @Transactional
    public List<PokedexEntryDto> listAllForUser(String userId) {
        ProfileModel profile = requireProfile(userId);
        userPokedexService.syncFromOwnership(profile);
        Set<Integer> registered = userPokedexService.findRegisteredPokedexNumbers(userId);
        return nationalPokedexCatalog.allSpeciesOrdered().stream()
                .map(p -> PokedexEntryDto.from(p, registered.contains(p.getPokedexNumber())))
                .toList();
    }

    /**
     * Sincroniza a Pokédex pessoal e devolve a página nacional (mesma transação de escrita).
     */
    @Transactional
    public PokedexEntryPageResponse listPageForUser(String userId, int page, int size, String query) {
        ProfileModel profile = requireProfile(userId);
        int safePage = Math.max(page, 0);
        if (safePage == 0) {
            userPokedexService.syncFromOwnership(profile);
        }
        Set<Integer> registered = userPokedexService.findRegisteredPokedexNumbers(userId);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "pokedexNumber")
        );
        Page<PokemonModel> speciesPage = searchSpeciesPage(query, pageable);
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

    private Page<PokemonModel> searchSpeciesPage(String query, Pageable pageable) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.isEmpty()) {
            return pokemonRepository.findAll(pageable);
        }

        String dexPart = trimmed.startsWith("#") ? trimmed.substring(1).trim() : trimmed;
        if (!dexPart.isEmpty() && dexPart.chars().allMatch(Character::isDigit)) {
            return pokemonRepository.searchByPokedexNumberPrefix(dexPart, pageable);
        }

        return pokemonRepository.findByNameContainingIgnoreCase(trimmed, pageable);
    }

    private ProfileModel requireProfile(String userId) {
        return profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
    }
}
