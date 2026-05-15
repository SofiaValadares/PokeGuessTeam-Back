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

import java.util.List;
import java.util.Set;

@Service
public class PokedexService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final PokemonRepository pokemonRepository;
    private final ProfileRepository profileRepository;
    private final UserPokedexService userPokedexService;

    public PokedexService(
            PokemonRepository pokemonRepository,
            ProfileRepository profileRepository,
            UserPokedexService userPokedexService
    ) {
        this.pokemonRepository = pokemonRepository;
        this.profileRepository = profileRepository;
        this.userPokedexService = userPokedexService;
    }

    @Transactional
    public List<PokedexEntryDto> listAllForUser(String userId) {
        ProfileModel profile = requireProfile(userId);
        userPokedexService.syncFromOwnership(profile);
        Set<Integer> registered = userPokedexService.findRegisteredPokedexNumbers(userId);
        return pokemonRepository.findAllByOrderByPokedexNumberAsc().stream()
                .map(p -> PokedexEntryDto.from(p, registered.contains(p.getPokedexNumber())))
                .toList();
    }

    /**
     * Sincroniza a Pokédex pessoal e devolve a página nacional (mesma transação de escrita).
     */
    @Transactional
    public PokedexEntryPageResponse listPageForUser(String userId, int page, int size) {
        ProfileModel profile = requireProfile(userId);
        userPokedexService.syncFromOwnership(profile);
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

    private ProfileModel requireProfile(String userId) {
        return profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
    }
}
