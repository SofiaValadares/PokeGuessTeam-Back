package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonType;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public record OpponentKnowledgeSlot(
            Integer pokedexNumber,
            boolean revealed,
            String primaryType,
            String secondaryType,
            String color,
            String generation,
            String heightM,
            String weightKg
    ) {
    }

    public static List<Integer> buildRandomTeam(
            List<PokemonModel> allPokemon,
            Set<Integer> excludedDexNumbers,
            int teamSize
    ) {
        List<PokemonModel> pool = allPokemon.stream()
                .filter(p -> !excludedDexNumbers.contains(p.getPokedexNumber()))
                .toList();
        List<PokemonModel> safePool = pool.size() >= teamSize ? pool : allPokemon;
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

        List<OpponentKnowledgeSlot> knownSlots = buildOpponentKnowledge(match, botSide, pokemonByDex);
        int topScore = -1;
        List<PokemonModel> topCandidates = new ArrayList<>();

        for (PokemonModel candidate : available) {
            int score = knownSlots.stream()
                    .mapToInt(slot -> scoreCandidateForSlot(candidate, slot))
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

    public static List<OpponentKnowledgeSlot> buildOpponentKnowledge(
            ActiveMatchModel match,
            MatchPlayerSide viewerSide,
            Map<Integer, PokemonModel> pokemonByDex
    ) {
        ActiveMatchPlayerModel viewer = match.getPlayer(viewerSide);
        ActiveMatchPlayerModel opponent = match.getOpponent(viewerSide);
        Set<Integer> hitSet = new HashSet<>(viewer.getHits());

        List<PokemonModel> opponentTeam = opponent.getTeam().stream()
                .map(pokemonByDex::get)
                .filter(Objects::nonNull)
                .toList();

        List<ActiveMatchGuessModel> viewerGuesses = match.getGuesses().stream()
                .filter(g -> g.getPlayerSide() == viewerSide)
                .sorted(Comparator.comparing(ActiveMatchGuessModel::getCreatedAt))
                .toList();

        List<OpponentKnowledgeSlot> slots = new ArrayList<>();
        for (PokemonModel opponentPokemon : opponentTeam) {
            boolean revealed = hitSet.contains(opponentPokemon.getPokedexNumber());
            String primaryType = null;
            String secondaryType = null;
            String color = null;
            String generation = null;
            String heightM = null;
            String weightKg = null;

            for (ActiveMatchGuessModel record : viewerGuesses) {
                PokemonModel guessed = pokemonByDex.get(record.getGuessedPokedexNumber());
                if (guessed == null) {
                    continue;
                }
                if (guessed.getPrimaryType() == opponentPokemon.getPrimaryType()) {
                    primaryType = opponentPokemon.getPrimaryType().name();
                }
                secondaryType = resolveSecondaryTypeHint(guessed, opponentPokemon, secondaryType);
                if (guessed.getColor() == opponentPokemon.getColor()) {
                    color = formatColor(opponentPokemon.getColor());
                }
                if (Objects.equals(guessed.getGeneration(), opponentPokemon.getGeneration())) {
                    generation = String.format("%02d", opponentPokemon.getGeneration());
                }
                if (Objects.equals(guessed.getHeightM(), opponentPokemon.getHeightM())) {
                    heightM = String.valueOf(opponentPokemon.getHeightM());
                }
                if (Objects.equals(guessed.getWeightKg(), opponentPokemon.getWeightKg())) {
                    weightKg = String.valueOf(opponentPokemon.getWeightKg());
                }
            }

            slots.add(new OpponentKnowledgeSlot(
                    revealed ? opponentPokemon.getPokedexNumber() : null,
                    revealed,
                    primaryType,
                    secondaryType,
                    color,
                    generation,
                    heightM,
                    weightKg
            ));
        }
        return slots;
    }

    private static String resolveSecondaryTypeHint(
            PokemonModel guessed,
            PokemonModel opponent,
            String current
    ) {
        if (guessed.getSecondaryType() == null && opponent.getSecondaryType() == null) {
            return "NENHUM";
        }
        if (guessed.getSecondaryType() != null
                && guessed.getSecondaryType() == opponent.getSecondaryType()) {
            return opponent.getSecondaryType().name();
        }
        return current;
    }

    private static String formatColor(PokedexColor color) {
        return color != null ? color.name() : null;
    }

    private static int scoreCandidateForSlot(PokemonModel pokemon, OpponentKnowledgeSlot slot) {
        if (slot == null || slot.revealed()) {
            return 0;
        }
        int score = 0;

        if (slot.primaryType() != null) {
            if (!pokemon.getPrimaryType().name().equalsIgnoreCase(slot.primaryType())) {
                return -1;
            }
            score += 4;
        }
        if (slot.secondaryType() != null) {
            String normalized = normalizeSecondaryType(pokemon.getSecondaryType());
            if (!normalized.equalsIgnoreCase(slot.secondaryType())) {
                return -1;
            }
            score += 4;
        }
        if (slot.color() != null) {
            if (pokemon.getColor() == null || !pokemon.getColor().name().equalsIgnoreCase(slot.color())) {
                return -1;
            }
            score += 3;
        }
        if (slot.generation() != null) {
            if (!String.format("%02d", pokemon.getGeneration()).equals(slot.generation())) {
                return -1;
            }
            score += 2;
        }
        if (slot.heightM() != null) {
            if (!String.valueOf(pokemon.getHeightM()).equals(slot.heightM())) {
                return -1;
            }
            score += 2;
        }
        if (slot.weightKg() != null) {
            if (!String.valueOf(pokemon.getWeightKg()).equals(slot.weightKg())) {
                return -1;
            }
            score += 2;
        }
        return score + countKnownFields(slot);
    }

    private static int countKnownFields(OpponentKnowledgeSlot slot) {
        int count = 0;
        if (slot.primaryType() != null) count++;
        if (slot.secondaryType() != null) count++;
        if (slot.color() != null) count++;
        if (slot.generation() != null) count++;
        if (slot.heightM() != null) count++;
        if (slot.weightKg() != null) count++;
        return count;
    }

    private static String normalizeSecondaryType(PokemonType type) {
        return type != null ? type.name() : "NENHUM";
    }
}
