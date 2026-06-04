package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.TrainingTeamModel;
import com.svc.pokeguessteam.model.user.UserPokedexModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokedexRepository;
import com.svc.pokeguessteam.repository.user.UserPokemonInventoryRepository;
import com.svc.pokeguessteam.util.EvolutionLinePokedexUnlock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserPokedexService {

    private final ProfileRepository profileRepository;
    private final UserPokedexRepository userPokedexRepository;
    private final PokemonRepository pokemonRepository;
    private final UserPokemonInventoryRepository userPokemonInventoryRepository;

    public UserPokedexService(
            ProfileRepository profileRepository,
            UserPokedexRepository userPokedexRepository,
            PokemonRepository pokemonRepository,
            UserPokemonInventoryRepository userPokemonInventoryRepository
    ) {
        this.profileRepository = profileRepository;
        this.userPokedexRepository = userPokedexRepository;
        this.pokemonRepository = pokemonRepository;
        this.userPokemonInventoryRepository = userPokemonInventoryRepository;
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

    @Transactional
    public UserPokedexModel registerSpecies(String userId, int pokedexNumber) {
        ProfileModel profile = requireProfile(userId);
        return registerSpecies(profile, pokedexNumber);
    }

    @Transactional
    public UserPokedexModel registerSpecies(ProfileModel profile, int pokedexNumber) {
        PokemonModel pokemon = pokemonRepository.findByPokedexNumber(pokedexNumber)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.POKEMON_SPECIES_NOT_FOUND,
                        MessageKeys.POKEMON_SPECIES_NOT_FOUND
                ));
        return registerSpecies(profile, pokemon);
    }

    /**
     * Regista na Pokédex pessoal as espécies desbloqueadas pela linha no inventário
     * (forma base ao obter; evoluções quando o nível da linha atinge o requisito).
     */
    @Transactional
    public void registerUnlockedSpeciesForInventoryLine(ProfileModel profile, UserPokemonInventoryModel inventoryRow) {
        if (profile == null || inventoryRow == null || inventoryRow.getEvolutionLine() == null) {
            return;
        }
        Integer lineKey = inventoryRow.getEvolutionLine().getLineKey();
        List<PokemonModel> lineSpecies = pokemonRepository.findByEvolutionLine_LineKey(lineKey);
        List<PokemonModel> unlocked = EvolutionLinePokedexUnlock.speciesUnlockedAtInventoryLevel(
                lineSpecies,
                inventoryRow
        );
        for (PokemonModel species : unlocked) {
            registerSpecies(profile, species);
        }
    }

    @Transactional
    public void registerStarterSpecies(ProfileModel profile, int... pokedexNumbers) {
        for (int dex : pokedexNumbers) {
            registerSpeciesIfPresent(profile, dex);
        }
    }

    /**
     * Alinha a Pokédex pessoal com o inventário (linhas evolutivas) e o time de treino.
     */
    @Transactional
    public void syncFromOwnership(ProfileModel profile) {
        List<UserPokemonInventoryModel> lines = userPokemonInventoryRepository
                .findByProfile_IdOrderByEvolutionLine_LineKeyAsc(profile.getId());
        for (UserPokemonInventoryModel row : lines) {
            registerUnlockedSpeciesForInventoryLine(profile, row);
        }
        TrainingTeamModel team = profile.getTrainingTeam();
        if (team != null) {
            for (int i = 0; i < TrainingTeamModel.TEAM_SIZE; i++) {
                EvolutionLineModel slot = team.getSlot(i);
                if (slot != null) {
                    userPokemonInventoryRepository
                            .findByProfile_IdAndEvolutionLine_LineKey(profile.getId(), slot.getLineKey())
                            .ifPresent(row -> registerUnlockedSpeciesForInventoryLine(profile, row));
                }
            }
        }
    }

    @Transactional
    public void registerSpeciesIfPresent(ProfileModel profile, int pokedexNumber) {
        pokemonRepository.findByPokedexNumber(pokedexNumber)
                .ifPresent(pokemon -> registerSpecies(profile, pokemon));
    }

    private UserPokedexModel registerSpecies(ProfileModel profile, PokemonModel pokemon) {
        int pokedexNumber = pokemon.getPokedexNumber();
        UserPokedexModel entry = userPokedexRepository
                .findByProfile_IdAndPokemon_PokedexNumber(profile.getId(), pokedexNumber)
                .orElseGet(() -> {
                    UserPokedexModel row = new UserPokedexModel();
                    row.setProfile(profile);
                    row.setPokemon(pokemon);
                    row.setRegistered(true);
                    return row;
                });
        if (!entry.isRegistered()) {
            entry.setRegistered(true);
        }
        return userPokedexRepository.saveAndFlush(entry);
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
