package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.UserPokedexModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokedexRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserPokedexService {

    private final ProfileRepository profileRepository;
    private final UserPokedexRepository userPokedexRepository;
    private final PokemonRepository pokemonRepository;

    public UserPokedexService(
            ProfileRepository profileRepository,
            UserPokedexRepository userPokedexRepository,
            PokemonRepository pokemonRepository
    ) {
        this.profileRepository = profileRepository;
        this.userPokedexRepository = userPokedexRepository;
        this.pokemonRepository = pokemonRepository;
    }

    @Transactional(readOnly = true)
    public Set<Integer> findRegisteredPokedexNumbers(String userId) {
        ProfileModel profile = requireProfile(userId);
        return userPokedexRepository.findRegisteredPokedexNumbersByProfile_Id(profile.getId());
    }

    @Transactional(readOnly = true)
    public boolean isRegisteredInUserPokedex(String userId, int pokedexNumber) {
        ProfileModel profile = requireProfile(userId);
        return userPokedexRepository.findByProfile_IdAndPokemon_PokedexNumber(profile.getId(), pokedexNumber)
                .map(UserPokedexModel::isRegistered)
                .orElse(false);
    }

    /**
     * Marca a espécie como registada na Pokédex pessoal do jogador.
     */
    @Transactional
    public UserPokedexModel registerSpecies(String userId, int pokedexNumber) {
        ProfileModel profile = requireProfile(userId);
        PokemonModel pokemon = pokemonRepository.findByPokedexNumber(pokedexNumber)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.POKEMON_SPECIES_NOT_FOUND,
                        MessageKeys.POKEMON_SPECIES_NOT_FOUND
                ));
        UserPokedexModel entry = userPokedexRepository
                .findByProfile_IdAndPokemon_PokedexNumber(profile.getId(), pokedexNumber)
                .orElseGet(() -> {
                    UserPokedexModel row = new UserPokedexModel();
                    row.setProfile(profile);
                    row.setPokemon(pokemon);
                    return row;
                });
        entry.setRegistered(true);
        return userPokedexRepository.save(entry);
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
