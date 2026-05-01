package com.svc.pokeguessteam.model;

import com.svc.pokeguessteam.model.enums.PokemonRarity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_POKEMONS")
public class PokemonModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_POKEMON_ID")
    private String id;

    @Column(name = "POKEDEX_NUMBER", nullable = false, unique = true)
    private Integer pokedexNumber;

    @Column(name = "POKEMON_NAME", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "PRIMARY_TYPE", nullable = false, length = 40)
    private String primaryType;

    @Column(name = "SECONDARY_TYPE", length = 40)
    private String secondaryType;

    @Column(name = "GENERATION_NUMBER", nullable = false)
    private Integer generation;

    @Column(name = "POKEMON_COLOR", nullable = false, length = 40)
    private String color;

    @Column(name = "HEIGHT_DM", nullable = false)
    private Integer heightDm;

    @Column(name = "WEIGHT_HG", nullable = false)
    private Integer weightHg;

    @Enumerated(EnumType.STRING)
    @Column(name = "RARITY", nullable = false, length = 20)
    private PokemonRarity rarity;

    public String getId() {
        return id;
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

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String primaryType) {
        this.primaryType = primaryType;
    }

    public String getSecondaryType() {
        return secondaryType;
    }

    public void setSecondaryType(String secondaryType) {
        this.secondaryType = secondaryType;
    }

    public Integer getGeneration() {
        return generation;
    }

    public void setGeneration(Integer generation) {
        this.generation = generation;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getHeightDm() {
        return heightDm;
    }

    public void setHeightDm(Integer heightDm) {
        this.heightDm = heightDm;
    }

    public Integer getWeightHg() {
        return weightHg;
    }

    public void setWeightHg(Integer weightHg) {
        this.weightHg = weightHg;
    }

    public PokemonRarity getRarity() {
        return rarity;
    }

    public void setRarity(PokemonRarity rarity) {
        this.rarity = rarity;
    }
}
