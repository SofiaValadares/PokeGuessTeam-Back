package com.svc.pokeguessteam.model;

import com.svc.pokeguessteam.model.enums.EvolutionStage;
import com.svc.pokeguessteam.model.enums.PokedexColor;
import com.svc.pokeguessteam.model.enums.PokemonRarity;
import com.svc.pokeguessteam.model.enums.PokemonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "TB_POKEMONS",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_POKEMON_DEX",
                columnNames = {"POKEDEX_NUMBER"}
        )
)
public class PokemonModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_POKEMON_ID", nullable = false, updatable = false)
    private Integer idPokemon;

    @Column(name = "POKEDEX_NUMBER", nullable = false)
    private Integer pokedexNumber;

    @Column(name = "POKEMON_NAME", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRIMARY_TYPE", nullable = false, length = 40)
    private PokemonType primaryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "SECONDARY_TYPE", length = 40)
    private PokemonType secondaryType;

    @Column(name = "GENERATION_NUMBER", nullable = false)
    private Integer generation;

    @Enumerated(EnumType.STRING)
    @Column(name = "POKEMON_COLOR", nullable = false, length = 20)
    private PokedexColor color;

    @Column(name = "HEIGHT_M", nullable = false)
    private Double heightM;

    @Column(name = "WEIGHT_KG", nullable = false)
    private Double weightKg;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "FK_EVOLUTION_LINE_ID", referencedColumnName = "LINE_KEY", nullable = false)
    private EvolutionLineModel evolutionLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVOLUTION_STAGE", length = 30)
    private EvolutionStage evolutionStage;

    @Column(name = "EVOLUTION_LEVEL")
    private Integer evolutionLevel;

    public String getIdPokemon() {
        return idPokemon;
    }

    public void setIdPokemon(String idPokemon) {
        this.idPokemon = idPokemon;
    }

    public Integer getPokedexNumber() {
        return pokedexNumber;
    }

    public void setPokedexNumber(Integer pokedexNumber) {
        this.pokedexNumber = pokedexNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PokemonType getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(PokemonType primaryType) {
        this.primaryType = primaryType;
    }

    public PokemonType getSecondaryType() {
        return secondaryType;
    }

    public void setSecondaryType(PokemonType secondaryType) {
        this.secondaryType = secondaryType;
    }

    public Integer getGeneration() {
        return generation;
    }

    public void setGeneration(Integer generation) {
        this.generation = generation;
    }

    public PokedexColor getColor() {
        return color;
    }

    public void setColor(PokedexColor color) {
        this.color = color;
    }

    public Double getHeightM() {
        return heightM;
    }

    public void setHeightM(Double heightM) {
        this.heightM = heightM;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public EvolutionLineModel getEvolutionLine() {
        return evolutionLine;
    }

    public void setEvolutionLine(EvolutionLineModel evolutionLine) {
        this.evolutionLine = evolutionLine;
    }

    public PokemonRarity getRarity() {
        return evolutionLine != null ? evolutionLine.getRarity() : null;
    }

    public EvolutionStage getEvolutionStage() {
        return evolutionStage;
    }

    public void setEvolutionStage(EvolutionStage evolutionStage) {
        this.evolutionStage = evolutionStage;
    }

    public Integer getEvolutionLevel() {
        return evolutionLevel;
    }

    public void setEvolutionLevel(Integer evolutionLevel) {
        this.evolutionLevel = evolutionLevel;
    }
}
