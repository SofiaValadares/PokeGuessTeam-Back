package com.svc.pokeguessteam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_PLAYER_POKEMONS")
public class PlayerPokemonModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_PLAYER_POKEMON_ID")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_PROFILE_ID", nullable = false)
    private ProfileModel profile;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "FK_POKEMON_ID", nullable = false)
    private PokemonModel pokemon;

    @Column(name = "LEVEL", nullable = false)
    private Integer level;

    @Column(name = "XP", nullable = false)
    private Integer xp;

    @Column(name = "IS_SHINY", nullable = false)
    private Boolean shiny;

    @Column(name = "DUPLICATE_COUNT", nullable = false)
    private Integer duplicateCount;

    public String getId() {
        return id;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void setProfile(ProfileModel profile) {
        this.profile = profile;
    }

    public PokemonModel getPokemon() {
        return pokemon;
    }

    public void setPokemon(PokemonModel pokemon) {
        this.pokemon = pokemon;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getXp() {
        return xp;
    }

    public void setXp(Integer xp) {
        this.xp = xp;
    }

    public Boolean getShiny() {
        return shiny;
    }

    public void setShiny(Boolean shiny) {
        this.shiny = shiny;
    }

    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }
}
