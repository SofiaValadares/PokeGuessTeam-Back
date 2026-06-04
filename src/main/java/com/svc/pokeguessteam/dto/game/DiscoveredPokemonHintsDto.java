package com.svc.pokeguessteam.dto.game;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonType;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;

import java.util.Objects;

/**
 * Pistas acumuladas sobre um slot do time adversário (campos omitidos = ainda não descobertos).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscoveredPokemonHintsDto(
        String nome,
        Integer numeroPokedex,
        String tipoPrimario,
        String tipoSecundario,
        Integer geracao,
        String cor,
        Double altura,
        Double peso,
        String estagioEvolutivo
) {
    public static final String TIPO_SECUNDARIO_NENHUM = "NENHUM";

    public static DiscoveredPokemonHintsDto empty() {
        return new DiscoveredPokemonHintsDto(null, null, null, null, null, null, null, null, null);
    }

    public static DiscoveredPokemonHintsDto fullyRevealed(PokemonModel pokemon) {
        return new DiscoveredPokemonHintsDto(
                pokemon.getName(),
                pokemon.getPokedexNumber(),
                pokemon.getPrimaryType() != null ? pokemon.getPrimaryType().name() : null,
                formatSecondaryType(pokemon.getSecondaryType()),
                pokemon.getGeneration(),
                pokemon.getColor() != null ? pokemon.getColor().name() : null,
                pokemon.getHeightM(),
                pokemon.getWeightKg(),
                pokemon.getEvolutionStage() != null ? pokemon.getEvolutionStage().name() : null
        );
    }

    public DiscoveredPokemonHintsDto merge(DiscoveredPokemonHintsDto other) {
        if (other == null) {
            return this;
        }
        return new DiscoveredPokemonHintsDto(
                pick(nome, other.nome),
                pick(numeroPokedex, other.numeroPokedex),
                pick(tipoPrimario, other.tipoPrimario),
                pick(tipoSecundario, other.tipoSecundario),
                pick(geracao, other.geracao),
                pick(cor, other.cor),
                pick(altura, other.altura),
                pick(peso, other.peso),
                pick(estagioEvolutivo, other.estagioEvolutivo)
        );
    }

    private static <T> T pick(T current, T incoming) {
        return current != null ? current : incoming;
    }

    public static String formatSecondaryType(PokemonType type) {
        return type != null ? type.name() : TIPO_SECUNDARIO_NENHUM;
    }

    public static String formatColor(PokedexColor color) {
        return color != null ? color.name() : null;
    }

    public static boolean sameSecondaryType(PokemonModel guessed, PokemonModel opponent) {
        if (guessed.getSecondaryType() == null && opponent.getSecondaryType() == null) {
            return true;
        }
        return guessed.getSecondaryType() != null
                && guessed.getSecondaryType() == opponent.getSecondaryType();
    }

    public static boolean matchesGeneration(PokemonModel guessed, PokemonModel opponent) {
        return Objects.equals(guessed.getGeneration(), opponent.getGeneration());
    }

    public static boolean matchesHeight(PokemonModel guessed, PokemonModel opponent) {
        return Objects.equals(guessed.getHeightM(), opponent.getHeightM());
    }

    public static boolean matchesWeight(PokemonModel guessed, PokemonModel opponent) {
        return Objects.equals(guessed.getWeightKg(), opponent.getWeightKg());
    }

    public static boolean matchesColor(PokemonModel guessed, PokemonModel opponent) {
        return guessed.getColor() == opponent.getColor();
    }

    public static boolean matchesEvolutionStage(PokemonModel guessed, PokemonModel opponent) {
        return guessed.getEvolutionStage() != null
                && guessed.getEvolutionStage() == opponent.getEvolutionStage();
    }
}
