package com.svc.pokeguessteam.model.user;

import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Registo na Pokédex pessoal do jogador: uma linha por espécie (número nacional) e perfil.
 */
@Entity
@Table(
        name = "TB_USER_POKEDEX",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_USER_POKEDEX_PROFILE_SPECIES",
                columnNames = {"FK_PROFILE_ID", "FK_POKEDEX_NUMBER"}
        )
)
public class UserPokedexModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_USER_POKEDEX_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_PROFILE_ID", nullable = false)
    private ProfileModel profile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "FK_POKEDEX_NUMBER",
            referencedColumnName = "POKEDEX_NUMBER",
            nullable = false
    )
    private PokemonModel pokemon;

    /** Espécie registada na Pokédex pessoal. */
    @Column(name = "REGISTERED", nullable = false)
    private boolean registered;

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

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}
