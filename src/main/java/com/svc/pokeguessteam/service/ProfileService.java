package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.PlayerPokemonModel;
import com.svc.pokeguessteam.model.PokemonModel;
import com.svc.pokeguessteam.model.ProfileModel;
import com.svc.pokeguessteam.model.UserModel;
import com.svc.pokeguessteam.repository.PlayerPokemonRepository;
import com.svc.pokeguessteam.repository.PokemonRepository;
import com.svc.pokeguessteam.repository.ProfileRepository;
import com.svc.pokeguessteam.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PokemonRepository pokemonRepository;
    private final PlayerPokemonRepository playerPokemonRepository;

    public ProfileService(ProfileRepository profileRepository,
                          UserRepository userRepository,
                          PokemonRepository pokemonRepository,
                          PlayerPokemonRepository playerPokemonRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.pokemonRepository = pokemonRepository;
        this.playerPokemonRepository = playerPokemonRepository;
    }

    @Transactional
    public ProfileModel ensureProfileWithStarters(String userId) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId).orElseGet(() -> createProfile(userId));
        grantStarterIfMissing(profile, 1);
        grantStarterIfMissing(profile, 4);
        grantStarterIfMissing(profile, 7);
        return profile;
    }

    private ProfileModel createProfile(String userId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_USER_NOT_FOUND,
                        MessageKeys.PROFILE_USER_NOT_FOUND
                ));
        ProfileModel profile = new ProfileModel();
        profile.setUser(user);
        return profileRepository.save(profile);
    }

    private void grantStarterIfMissing(ProfileModel profile, Integer pokedexNumber) {
        PokemonModel pokemon = pokemonRepository
                .findByPokedexNumber(pokedexNumber)
                .orElse(null);
        if (pokemon == null) {
            return;
        }
        boolean alreadyOwned = playerPokemonRepository
                .findByProfile_IdAndPokemon_PokedexNumber(profile.getId(), pokemon.getPokedexNumber())
                .isPresent();
        if (alreadyOwned) {
            return;
        }
        PlayerPokemonModel owned = new PlayerPokemonModel();
        owned.setProfile(profile);
        owned.setPokemon(pokemon);
        owned.setLevel(1);
        owned.setXp(0);
        owned.setShiny(false);
        owned.setDuplicateCount(0);
        playerPokemonRepository.save(owned);
    }

    public List<PlayerPokemonModel> getCollection(String userId) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        return playerPokemonRepository.findByProfile_Id(profile.getId());
    }
}
