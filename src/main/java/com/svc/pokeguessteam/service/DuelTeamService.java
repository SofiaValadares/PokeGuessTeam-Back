package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.model.user.ProfileModel;
import com.svc.pokeguessteam.model.user.UserPokemonInventoryModel;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DuelTeamService {

    private final UserPokedexService userPokedexService;
    private final NationalPokedexCatalog nationalPokedexCatalog;
    private final ProfileRepository profileRepository;
    private final UserPokemonInventoryRepository inventoryRepository;

    public DuelTeamService(
            UserPokedexService userPokedexService,
            NationalPokedexCatalog nationalPokedexCatalog,
            ProfileRepository profileRepository,
            UserPokemonInventoryRepository inventoryRepository
    ) {
        this.userPokedexService = userPokedexService;
        this.nationalPokedexCatalog = nationalPokedexCatalog;
        this.profileRepository = profileRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public Set<Integer> registeredNumbers(String userId) {
        return userPokedexService.findRegisteredPokedexNumbers(userId);
    }

    @Transactional(readOnly = true)
    public void requireRegisteredGuess(String userId, int pokedexNumber) {
        if (!registeredNumbers(userId).contains(pokedexNumber)) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_GUESS_NOT_IN_POKEDEX,
                    MessageKeys.GAME_GUESS_NOT_IN_POKEDEX
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Integer> validateTeamFromRegisteredPokedex(String userId, List<Integer> team) {
        return validateTeam(registeredNumbers(userId), team, null);
    }

    /**
     * Fonte de verdade da equipa de duelo: 6 espécies únicas, na Pokédex do user
     * (e no pool do evento, se {@code eventAllowlist} for não-nulo) e existentes no catálogo.
     */
    @Transactional(readOnly = true)
    public List<Integer> validateTeamFromRegisteredPokedex(
            String userId,
            List<Integer> team,
            Set<Integer> eventAllowlist
    ) {
        return validateTeam(registeredNumbers(userId), team, eventAllowlist);
    }

    @Transactional(readOnly = true)
    public List<Integer> validateTeam(Set<Integer> registered, List<Integer> team, Set<Integer> eventAllowlist) {
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

        Set<Integer> pool = registered;
        if (eventAllowlist != null) {
            pool = new HashSet<>(registered);
            pool.retainAll(eventAllowlist);
            if (pool.size() < GameConstants.TEAM_SIZE) {
                throw new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.GAME_EVENT_POOL_INSUFFICIENT,
                        MessageKeys.GAME_EVENT_POOL_INSUFFICIENT,
                        GameConstants.TEAM_SIZE,
                        pool.size()
                );
            }
        } else if (registered.size() < GameConstants.TEAM_SIZE) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_POKEDEX_INSUFFICIENT,
                    MessageKeys.GAME_POKEDEX_INSUFFICIENT,
                    GameConstants.TEAM_SIZE,
                    registered.size()
            );
        }

        Map<Integer, PokemonModel> byDex = nationalPokedexCatalog.speciesByDex();
        for (Integer dex : team) {
            if (!registered.contains(dex)) {
                throw new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.GAME_TEAM_NOT_IN_POKEDEX,
                        MessageKeys.GAME_TEAM_NOT_IN_POKEDEX
                );
            }
            if (eventAllowlist != null && !eventAllowlist.contains(dex)) {
                throw new ApiBusinessException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCodes.GAME_TEAM_NOT_IN_EVENT,
                        MessageKeys.GAME_TEAM_NOT_IN_EVENT
                );
            }
            if (!byDex.containsKey(dex)) {
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
        Map<Integer, PokemonModel> byDex = nationalPokedexCatalog.speciesByDex();

        List<Integer> pcForms = new ArrayList<>();
        for (UserPokemonInventoryModel row : lines) {
            if (row.getEvolutionLine() == null) {
                continue;
            }
            List<Integer> members = row.getEvolutionLine().getMemberPokedexNumbers();
            if (members.isEmpty()) {
                continue;
            }
            List<PokemonModel> lineSpecies = new ArrayList<>();
            for (Integer member : members) {
                PokemonModel species = byDex.get(member);
                if (species != null) {
                    lineSpecies.add(species);
                }
            }
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

        Set<Integer> registered = registeredNumbers(userId);
        if (team.size() < GameConstants.TEAM_SIZE) {
            List<PokemonModel> fillPool = modelsFromRegistered(registered).stream()
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
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_POKEDEX_INSUFFICIENT,
                    MessageKeys.GAME_POKEDEX_INSUFFICIENT,
                    GameConstants.TEAM_SIZE * 2,
                    registered.size()
            );
        }
        return List.copyOf(team);
    }

    @Transactional(readOnly = true)
    public List<Integer> buildBotTeamFromUserPokedex(String userId, Set<Integer> excludedDexNumbers) {
        return buildBotTeamFromRegistered(registeredNumbers(userId), excludedDexNumbers);
    }

    @Transactional(readOnly = true)
    public List<Integer> buildBotTeamFromRegistered(Set<Integer> registered, Set<Integer> excludedDexNumbers) {
        List<PokemonModel> pool = modelsFromRegistered(registered);
        try {
            return BotAiOpponent.buildRandomTeam(pool, excludedDexNumbers, GameConstants.TEAM_SIZE, true);
        } catch (IllegalStateException ex) {
            throw new ApiBusinessException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.GAME_POKEDEX_INSUFFICIENT,
                    MessageKeys.GAME_POKEDEX_INSUFFICIENT,
                    GameConstants.TEAM_SIZE * 2,
                    registered.size()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<PokemonModel> registeredPokemonModels(String userId) {
        return modelsFromRegistered(registeredNumbers(userId));
    }

    public List<PokemonModel> modelsFromRegistered(Set<Integer> registered) {
        if (registered == null || registered.isEmpty()) {
            return List.of();
        }
        Map<Integer, PokemonModel> byDex = nationalPokedexCatalog.speciesByDex();
        List<PokemonModel> found = new ArrayList<>();
        registered.stream().sorted().forEach(dex -> {
            PokemonModel pokemon = byDex.get(dex);
            if (pokemon != null) {
                found.add(pokemon);
            }
        });
        return List.copyOf(found);
    }

    @Transactional(readOnly = true)
    public int countRegistered(String userId) {
        return registeredNumbers(userId).size();
    }
}
