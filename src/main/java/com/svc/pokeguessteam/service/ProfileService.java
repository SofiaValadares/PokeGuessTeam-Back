package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.enums.PokeballType;
import com.svc.pokeguessteam.model.user.ProfileInventoryItemModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.TrainingTeamModel;
import com.svc.pokeguessteam.model.user.UserModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.repository.user.ProfileInventoryItemRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokemonInventoryRepository;
import com.svc.pokeguessteam.repository.user.UserRepository;
import com.svc.pokeguessteam.util.PokemonInventoryXp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProfileService {

    /** Fragmentos de Poké Bola necessários para ganhar 1 Poké Bola. */
    public static final int FRAGMENTS_PER_POKE_BALL = 10;

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
    private final ProfileInventoryItemRepository profileInventoryItemRepository;

    public ProfileService(ProfileRepository profileRepository,
                          UserRepository userRepository,
                          PokemonRepository pokemonRepository,
                          UserPokemonInventoryRepository inventoryRepository,
                          ProfileInventoryItemRepository profileInventoryItemRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.pokemonRepository = pokemonRepository;
        this.inventoryRepository = inventoryRepository;
        this.profileInventoryItemRepository = profileInventoryItemRepository;
    }

    @Transactional
    public ProfileModel ensureProfileWithStarters(String userId) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId).orElseGet(() -> createProfile(userId));
        for (int dex : STARTER_POKEDEX_NUMBERS) {
            grantStarterLineIfMissing(profile, dex);
        }
        ensurePokeballInventoryIfMissing(profile);
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
        profile.setPokeballFragments(0);
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

    /**
     * Garante uma linha por tipo de Pokébola (quantidade inicial 0) quando o registo ainda não existe.
     */
    private void ensurePokeballInventoryIfMissing(ProfileModel profile) {
        for (PokeballType type : PokeballType.values()) {
            if (profileInventoryItemRepository.findByProfile_IdAndPokeballType(profile.getId(), type).isPresent()) {
                continue;
            }
            ProfileInventoryItemModel row = new ProfileInventoryItemModel();
            row.setProfile(profile);
            row.setPokeballType(type);
            row.setQuantity(0);
            profileInventoryItemRepository.save(row);
        }
    }

    /**
     * Acrescenta fragmentos de Poké Bola; de 10 em 10 converte automaticamente em 1 Poké Bola no inventário.
     */
    @Transactional
    public void addPokeballFragments(String userId, int amount) {
        if (amount <= 0) {
            return;
        }
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        ensurePokeballInventoryIfMissing(profile);
        int current = profile.getPokeballFragments() != null ? profile.getPokeballFragments() : 0;
        profile.setPokeballFragments(current + amount);
        convertPokeballFragmentsToBalls(profile);
        profileRepository.save(profile);
    }

    private void convertPokeballFragmentsToBalls(ProfileModel profile) {
        int frags = profile.getPokeballFragments() != null ? profile.getPokeballFragments() : 0;
        if (frags < FRAGMENTS_PER_POKE_BALL) {
            return;
        }
        ProfileInventoryItemModel pokeRow = profileInventoryItemRepository
                .findByProfile_IdAndPokeballType(profile.getId(), PokeballType.POKE_BALL)
                .orElseThrow();
        int newBalls = frags / FRAGMENTS_PER_POKE_BALL;
        int remainder = frags % FRAGMENTS_PER_POKE_BALL;
        int qty = pokeRow.getQuantity() != null ? pokeRow.getQuantity() : 0;
        pokeRow.setQuantity(qty + newBalls);
        profile.setPokeballFragments(remainder);
        profileInventoryItemRepository.save(pokeRow);
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
    public List<ProfileInventoryItemModel> getItemInventory(String userId) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));
        return listInventoryItemsOrdered(profile.getId());
    }

    @Transactional(readOnly = true)
    public List<ProfileInventoryItemModel> getItemInventoryByProfileId(String profileId) {
        return listInventoryItemsOrdered(profileId);
    }

    private List<ProfileInventoryItemModel> listInventoryItemsOrdered(String profileId) {
        return profileInventoryItemRepository.findByProfile_Id(profileId).stream()
                .sorted(Comparator.comparingInt(a -> a.getPokeballType().ordinal()))
                .toList();
    }
}
  
