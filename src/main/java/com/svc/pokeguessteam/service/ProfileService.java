package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.TrainingTeamModel;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokemonInventoryRepository;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.util.PokemonInventoryXp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProfileService {

    /**
     * Uma entrada por linha evolutiva: iniciais principais de cada geração (Pokédex nacional).
     */
    private static final int[] STARTER_POKEDEX_NUMBERS = {
            1, 4, 7,
            152, 155, 158,
            252, 255, 258,
            387, 390, 393,
            495, 498, 501,
            650, 653, 656,
            722, 725, 728,
            810, 813, 816,
            906, 909, 912,
    };

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PokemonRepository pokemonRepository;
    private final UserPokemonInventoryRepository inventoryRepository;

    public ProfileService(ProfileRepository profileRepository,
                          UserRepository userRepository,
                          PokemonRepository pokemonRepository,
                          UserPokemonInventoryRepository inventoryRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.pokemonRepository = pokemonRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public ProfileModel ensureProfileWithStarters(String userId) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId).orElseGet(() -> createProfile(userId));
        for (int dex : STARTER_POKEDEX_NUMBERS) {
            grantStarterLineIfMissing(profile, dex);
        }
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
        ProfileModel saved = profileRepository.save(profile);
        pokemonRepository.findByPokedexNumber(1).ifPresent(saved::setFavoritePokemon);
        assignRandomDefaultTrainingTeam(saved);
        return profileRepository.save(saved);
    }

    /**
     * Time de treino inicial com 6 espécies aleatórias (com reposição se existirem menos de 6 na base).
     */
    private void assignRandomDefaultTrainingTeam(ProfileModel profile) {
        List<PokemonModel> pool = pokemonRepository.findAllByOrderByPokedexNumberAsc();
        if (pool.isEmpty()) {
            return;
        }
        List<PokemonModel> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        TrainingTeamModel team = new TrainingTeamModel();
        team.setProfile(profile);
        for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
            team.setSlot(i, shuffled.get(i % shuffled.size()));
        }
        profile.setTrainingTeam(team);
    }

    private void grantStarterLineIfMissing(ProfileModel profile, int pokedexNumber) {
        PokemonModel pokemon = pokemonRepository.findByPokedexNumber(pokedexNumber).orElse(null);
        if (pokemon == null) {
            return;
        }
        EvolutionLineModel line = pokemon.getEvolutionLine();
        if (inventoryRepository.findByProfile_IdAndEvolutionLine_LineKey(profile.getId(), line.getLineKey())
                .isPresent()) {
            return;
        }
        UserPokemonInventoryModel row = new UserPokemonInventoryModel();
        row.setProfile(profile);
        row.setEvolutionLine(line);
        row.setTotalXp(0);
        row.setTimesObtained(1);
        PokemonInventoryXp.syncLevelFromTotalXp(row);
        inventoryRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<UserPokemonInventoryModel> getInventory(String userId) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        return inventoryRepository.findByProfile_IdOrderByEvolutionLine_LineKeyAsc(profile.getId());
    }
}
  
