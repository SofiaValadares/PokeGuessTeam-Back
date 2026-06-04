package com.svc.pokeguessteam.util;

import com.svc.pokeguessteam.dto.game.DiscoveredPokemonHintsDto;
import com.svc.pokeguessteam.dto.game.OpponentSlotKnowledgeDto;
import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.game.ActiveMatchGuessModel;
import com.svc.pokeguessteam.model.game.ActiveMatchModel;
import com.svc.pokeguessteam.model.game.ActiveMatchPlayerModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Monta as 6 posições do time adversário com pistas acumuladas por palpite.
 */
public final class OpponentKnowledgeBuilder {

    private OpponentKnowledgeBuilder() {
    }

    public static List<OpponentSlotKnowledgeDto> buildTeamKnowledge(
            ActiveMatchModel match,
            MatchPlayerSide viewerSide,
            Map<Integer, PokemonModel> pokemonByDex
    ) {
        ActiveMatchPlayerModel viewer = match.getPlayer(viewerSide);
        ActiveMatchPlayerModel opponent = match.getOpponent(viewerSide);
        Set<Integer> hitSet = new HashSet<>(viewer.getHits());

        List<Integer> opponentTeamDex = opponent.getTeam();
        List<ActiveMatchGuessModel> viewerGuesses = match.getGuesses().stream()
                .filter(g -> g.getPlayerSide() == viewerSide)
                .sorted(Comparator.comparing(
                        ActiveMatchGuessModel::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        List<OpponentSlotKnowledgeDto> slots = new ArrayList<>(GameConstants.TEAM_SIZE);
        for (int i = 0; i < GameConstants.TEAM_SIZE; i++) {
            int slotNumber = i + 1;
            if (i >= opponentTeamDex.size()) {
                slots.add(new OpponentSlotKnowledgeDto(slotNumber, false, DiscoveredPokemonHintsDto.empty()));
                continue;
            }
            PokemonModel opponentPokemon = pokemonByDex.get(opponentTeamDex.get(i));
            if (opponentPokemon == null) {
                slots.add(new OpponentSlotKnowledgeDto(slotNumber, false, DiscoveredPokemonHintsDto.empty()));
                continue;
            }

            boolean adivinhado = hitSet.contains(opponentPokemon.getPokedexNumber());
            DiscoveredPokemonHintsDto informacoes = adivinhado
                    ? DiscoveredPokemonHintsDto.fullyRevealed(opponentPokemon)
                    : accumulateHints(viewerGuesses, opponentPokemon, pokemonByDex);

            slots.add(new OpponentSlotKnowledgeDto(slotNumber, adivinhado, informacoes));
        }
        return slots;
    }

    private static DiscoveredPokemonHintsDto accumulateHints(
            List<ActiveMatchGuessModel> viewerGuesses,
            PokemonModel opponentPokemon,
            Map<Integer, PokemonModel> pokemonByDex
    ) {
        DiscoveredPokemonHintsDto merged = DiscoveredPokemonHintsDto.empty();
        for (ActiveMatchGuessModel record : viewerGuesses) {
            PokemonModel guessed = pokemonByDex.get(record.getGuessedPokedexNumber());
            if (guessed == null) {
                continue;
            }
            merged = merged.merge(hintsFromGuess(guessed, opponentPokemon));
        }
        return merged;
    }

    private static DiscoveredPokemonHintsDto hintsFromGuess(PokemonModel guessed, PokemonModel opponent) {
        DiscoveredPokemonHintsDto hints = DiscoveredPokemonHintsDto.empty();

        if (guessed.getPrimaryType() == opponent.getPrimaryType()) {
            hints = hints.merge(new DiscoveredPokemonHintsDto(
                    null, null, opponent.getPrimaryType().name(), null, null, null, null, null, null
            ));
        }

        if (DiscoveredPokemonHintsDto.sameSecondaryType(guessed, opponent)) {
            hints = hints.merge(new DiscoveredPokemonHintsDto(
                    null,
                    null,
                    null,
                    DiscoveredPokemonHintsDto.formatSecondaryType(opponent.getSecondaryType()),
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        if (DiscoveredPokemonHintsDto.matchesColor(guessed, opponent)) {
            hints = hints.merge(new DiscoveredPokemonHintsDto(
                    null, null, null, null, null,
                    DiscoveredPokemonHintsDto.formatColor(opponent.getColor()),
                    null, null, null
            ));
        }

        if (DiscoveredPokemonHintsDto.matchesGeneration(guessed, opponent)) {
            hints = hints.merge(new DiscoveredPokemonHintsDto(
                    null, null, null, null, opponent.getGeneration(), null, null, null, null
            ));
        }

        if (DiscoveredPokemonHintsDto.matchesHeight(guessed, opponent)) {
            hints = hints.merge(new DiscoveredPokemonHintsDto(
                    null, null, null, null, null, null, opponent.getHeightM(), null, null
            ));
        }

        if (DiscoveredPokemonHintsDto.matchesWeight(guessed, opponent)) {
            hints = hints.merge(new DiscoveredPokemonHintsDto(
                    null, null, null, null, null, null, null, opponent.getWeightKg(), null
            ));
        }

        if (DiscoveredPokemonHintsDto.matchesEvolutionStage(guessed, opponent)) {
            hints = hints.merge(new DiscoveredPokemonHintsDto(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    opponent.getEvolutionStage() != null ? opponent.getEvolutionStage().name() : null
            ));
        }

        return hints;
    }

    /** Pontuação para IA do bot (compatível com pistas parciais). */
    public static int scoreCandidateForSlot(PokemonModel pokemon, OpponentSlotKnowledgeDto slot) {
        if (slot == null || slot.adivinhado()) {
            return 0;
        }
        DiscoveredPokemonHintsDto info = slot.informacoes();
        if (info == null) {
            return 0;
        }
        int score = 0;

        if (info.tipoPrimario() != null) {
            if (!pokemon.getPrimaryType().name().equalsIgnoreCase(info.tipoPrimario())) {
                return -1;
            }
            score += 4;
        }
        if (info.tipoSecundario() != null) {
            String normalized = DiscoveredPokemonHintsDto.formatSecondaryType(pokemon.getSecondaryType());
            if (!normalized.equalsIgnoreCase(info.tipoSecundario())) {
                return -1;
            }
            score += 4;
        }
        if (info.cor() != null) {
            if (pokemon.getColor() == null || !pokemon.getColor().name().equalsIgnoreCase(info.cor())) {
                return -1;
            }
            score += 3;
        }
        if (info.geracao() != null) {
            if (!Objects.equals(pokemon.getGeneration(), info.geracao())) {
                return -1;
            }
            score += 2;
        }
        if (info.altura() != null) {
            if (!Objects.equals(pokemon.getHeightM(), info.altura())) {
                return -1;
            }
            score += 2;
        }
        if (info.peso() != null) {
            if (!Objects.equals(pokemon.getWeightKg(), info.peso())) {
                return -1;
            }
            score += 2;
        }
        if (info.estagioEvolutivo() != null) {
            if (pokemon.getEvolutionStage() == null
                    || !pokemon.getEvolutionStage().name().equalsIgnoreCase(info.estagioEvolutivo())) {
                return -1;
            }
            score += 2;
        }
        return score + countKnownFields(info);
    }

    private static int countKnownFields(DiscoveredPokemonHintsDto info) {
        int count = 0;
        if (info.tipoPrimario() != null) count++;
        if (info.tipoSecundario() != null) count++;
        if (info.cor() != null) count++;
        if (info.geracao() != null) count++;
        if (info.altura() != null) count++;
        if (info.peso() != null) count++;
        if (info.estagioEvolutivo() != null) count++;
        return count;
    }
}
