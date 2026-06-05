package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.dto.game.OpponentSlotKnowledgeDto;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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

    public static PokemonModel chooseGuess(
            List<PokemonModel> allPokemon,
            ActiveMatchModel match,
            MatchPlayerSide botSide,
            Map<Integer, PokemonModel> pokemonByDex
    ) {
        Set<Integer> guessedDex = match.getGuesses().stream()
                .filter(g -> g.getPlayerSide() == botSide)
                .map(ActiveMatchGuessModel::getGuessedPokedexNumber)
                .collect(Collectors.toSet());

        List<PokemonModel> available = allPokemon.stream()
                .filter(p -> !guessedDex.contains(p.getPokedexNumber()))
                .toList();
        if (available.isEmpty()) {
            return null;
        }

        List<OpponentSlotKnowledgeDto> knownSlots = OpponentKnowledgeBuilder.buildTeamKnowledge(
                match,
                botSide,
                pokemonByDex
        );
        int topScore = -1;
        List<PokemonModel> topCandidates = new ArrayList<>();

        for (PokemonModel candidate : available) {
            int score = knownSlots.stream()
                    .mapToInt(slot -> OpponentKnowledgeBuilder.scoreCandidateForSlot(candidate, slot))
                    .max()
                    .orElse(0);
            if (score > topScore) {
                topScore = score;
                topCandidates.clear();
                topCandidates.add(candidate);
            } else if (score == topScore) {
                topCandidates.add(candidate);
            }
        }

        if (topCandidates.isEmpty()) {
            return available.get(ThreadLocalRandom.current().nextInt(available.size()));
        }
        return topCandidates.get(ThreadLocalRandom.current().nextInt(topCandidates.size()));
    }
}
