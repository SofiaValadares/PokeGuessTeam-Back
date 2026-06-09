package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.repository.user.ProfileRepository;
import com.svc.pokeguessteam.repository.user.UserPokemonInventoryRepository;
import com.svc.pokeguessteam.util.BotAiOpponent;
import com.svc.pokeguessteam.util.EvolutionLinePokedexUnlock;
import com.svc.pokeguessteam.util.GameConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DuelTeamService {

    private final UserPokedexService userPokedexService;
    private final PokemonRepository pokemonRepository;
    private final ProfileRepository profileRepository;
    private final UserPokemonInventoryRepository inventoryRepository;

    public DuelTeamService(
            UserPokedexService userPokedexService,
            PokemonRepository pokemonRepository,
            ProfileRepository profileRepository,
            UserPokemonInventoryRepository inventoryRepository
    ) {
        this.userPokedexService = userPokedexService;
        this.pokemonRepository = pokemonRepository;
        this.profileRepository = profileRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Integer> validateTeamFromRegisteredPokedex(String userId, List<Integer> team) {
        if (team == null || team.size() != GameConstants.TEAM_SIZE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_TEAM_INVALID,
                    MessageKeys.GAME_TEAM_INVALID,
                    GameConstants.TEAM_SIZE
            );
        }
        Set<Integer> unique = new HashSet<>(team);
        if (unique.size() != GameConstants.TEAM_SIZE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_TEAM_INVALID,
                    MessageKeys.GAME_TEAM_DUPLICATE
            );
        }

        Set<Integer> registered = userPokedexService.findRegisteredPokedexNumbers(userId);
        if (registered.size() < GameConstants.TEAM_SIZE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_POKEDEX_INSUFFICIENT,
                    MessageKeys.GAME_POKEDEX_INSUFFICIENT,
                    GameConstants.TEAM_SIZE,
                    registered.size()
            );
        }

        for (Integer dex : team) {
            if (!registered.contains(dex)) {
                throw new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.GAME_TEAM_NOT_IN_POKEDEX,
                        MessageKeys.GAME_TEAM_NOT_IN_POKEDEX
                );
            }
            if (pokemonRepository.findByPokedexNumber(dex).isEmpty()) {
                throw new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.GAME_TEAM_INVALID,
                        MessageKeys.POKEMON_SPECIES_NOT_FOUND
                );
            }
        }
        return List.copyOf(team);
    }

    @Transactional(readOnly = true)
    public List<Integer> buildBotTeamFromUserPc(String userId, Set<Integer> excludedDexNumbers) {
        ProfileModel profile = profileRepository.findByUser_IdUser(userId)
                .orElseThrow(() -> new ApiBusinessException(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.PROFILE_NOT_FOUND,
                        MessageKeys.PROFILE_NOT_FOUND
                ));

        List<UserPokemonInventoryModel> lines = inventoryRepository
                .findByProfile_IdOrderByEvolutionLine_LineKeyAsc(profile.getId());

        List<Integer> pcForms = new ArrayList<>();
        for (UserPokemonInventoryModel row : lines) {
            if (row.getEvolutionLine() == null) {
                continue;
            }
            List<Integer> members = row.getEvolutionLine().getMemberPokedexNumbers();
            if (members.isEmpty()) {
                continue;
            }
            List<PokemonModel> lineSpecies = pokemonRepository.findByPokedexNumberIn(members);
            List<PokemonModel> unlocked = EvolutionLinePokedexUnlock.speciesUnlockedAtInventoryLevel(
                    lineSpecies,
                    row
            );
            if (unlocked.isEmpty()) {
                continue;
            }
            int dex = unlocked.get(unlocked.size() - 1).getPokedexNumber();
            if (!excludedDexNumbers.contains(dex)) {
                pcForms.add(dex);
            }
        }

        LinkedHashSet<Integer> uniqueForms = new LinkedHashSet<>(pcForms);
        List<Integer> shuffled = new ArrayList<>(uniqueForms);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());

        List<Integer> team = new ArrayList<>();
        Set<Integer> used = new HashSet<>(excludedDexNumbers);
        for (Integer dex : shuffled) {
            if (team.size() >= GameConstants.TEAM_SIZE) {
                break;
            }
            if (used.add(dex)) {
                team.add(dex);
            }
        }

        if (team.size() < GameConstants.TEAM_SIZE) {
            List<PokemonModel> fillPool = registeredPokemonModels(userId).stream()
                    .filter(p -> !used.contains(p.getPokedexNumber()))
                    .toList();
            List<PokemonModel> extra = new ArrayList<>(fillPool);
            Collections.shuffle(extra, ThreadLocalRandom.current());
            for (PokemonModel pokemon : extra) {
                if (team.size() >= GameConstants.TEAM_SIZE) {
                    break;
                }
                if (used.add(pokemon.getPokedexNumber())) {
                    team.add(pokemon.getPokedexNumber());
                }
            }
        }

        if (team.size() < GameConstants.TEAM_SIZE) {
            int registered = countRegistered(userId);
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_POKEDEX_INSUFFICIENT,
                    MessageKeys.GAME_POKEDEX_INSUFFICIENT,
                    GameConstants.TEAM_SIZE * 2,
                    registered
            );
        }
        return List.copyOf(team);
    }

    @Transactional(readOnly = true)
    public List<Integer> buildBotTeamFromUserPokedex(String userId, Set<Integer> excludedDexNumbers) {
        List<PokemonModel> pool = registeredPokemonModels(userId);
        try {
            return BotAiOpponent.buildRandomTeam(pool, excludedDexNumbers, GameConstants.TEAM_SIZE, true);
        } catch (IllegalStateException ex) {
            int registered = countRegistered(userId);
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_POKEDEX_INSUFFICIENT,
                    MessageKeys.GAME_POKEDEX_INSUFFICIENT,
                    GameConstants.TEAM_SIZE * 2,
                    registered
            );
        }
    }

    @Transactional(readOnly = true)
    public List<PokemonModel> registeredPokemonModels(String userId) {
        Set<Integer> registered = userPokedexService.findRegisteredPokedexNumbers(userId);
        return pokemonRepository.findAllByOrderByPokedexNumberAsc().stream()
                .filter(p -> registered.contains(p.getPokedexNumber()))
                .toList();
    }

    @Transactional(readOnly = true)
    public int countRegistered(String userId) {
        return userPokedexService.findRegisteredPokedexNumbers(userId).size();
    }
}
