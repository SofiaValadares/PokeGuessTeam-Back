package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.pokemon.PokemonModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * IA do bot e construção de pistas (alinhado à beta).
 */
public final class BotAiOpponent {

    public static final String BOT_NAME = "PokéBot";

    private BotAiOpponent() {
    }

    public static List<Integer> buildRandomTeam(
            List<PokemonModel> allPokemon,
            Set<Integer> excludedDexNumbers,
            int teamSize
    ) {
        return buildRandomTeam(allPokemon, excludedDexNumbers, teamSize, false);
    }

    public static List<Integer> buildRandomTeam(
            List<PokemonModel> allPokemon,
            Set<Integer> excludedDexNumbers,
            int teamSize,
            boolean strictPool
    ) {
        List<PokemonModel> pool = allPokemon.stream()
                .filter(p -> !excludedDexNumbers.contains(p.getPokedexNumber()))
                .toList();
        List<PokemonModel> safePool = strictPool || pool.size() >= teamSize ? pool : allPokemon;
        if (safePool.size() < teamSize) {
            throw new IllegalStateException("Pool insuficiente para montar equipa de " + teamSize);
        }
        List<PokemonModel> shuffled = new ArrayList<>(safePool);
        java.util.Collections.shuffle(shuffled, ThreadLocalRandom.current());
        return shuffled.stream()
                .limit(teamSize)
                .map(PokemonModel::getPokedexNumber)
                .toList();
    }
}
