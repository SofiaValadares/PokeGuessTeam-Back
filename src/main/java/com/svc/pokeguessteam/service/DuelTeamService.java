package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.exception.ApiBusinessException;
import com.svc.pokeguessteam.exception.ErrorCodes;
import com.svc.pokeguessteam.messages.MessageKeys;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import com.svc.pokeguessteam.repository.pokemon.PokemonRepository;
import com.svc.pokeguessteam.util.BotAiOpponent;
import com.svc.pokeguessteam.util.GameConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DuelTeamService {

    private final UserPokedexService userPokedexService;
    private final PokemonRepository pokemonRepository;

    public DuelTeamService(UserPokedexService userPokedexService, PokemonRepository pokemonRepository) {
        this.userPokedexService = userPokedexService;
        this.pokemonRepository = pokemonRepository;
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
