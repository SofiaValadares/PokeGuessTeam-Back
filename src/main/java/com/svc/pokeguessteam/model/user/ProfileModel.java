package com.svc.pokeguessteam.model.user;

import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TB_PROFILES")
public class ProfileModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_PROFILE_ID")
    private String id;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "FK_USER_ID", nullable = false, unique = true)
    private UserModel user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "FK_FAVORITE_POKEDEX_NUMBER",
            referencedColumnName = "POKEDEX_NUMBER",
            nullable = true
    )
    private PokemonModel favoritePokemon;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPokemonInventoryModel> inventory = new ArrayList<>();

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public PokemonModel getFavoritePokemon() {
        return favoritePokemon;
    }

    public void setFavoritePokemon(PokemonModel favoritePokemon) {
        this.favoritePokemon = favoritePokemon;
    }

    public List<UserPokemonInventoryModel> getInventory() {
        return inventory;
    }

    public void setInventory(List<UserPokemonInventoryModel> inventory) {
        this.inventory = inventory != null ? inventory : new ArrayList<>();
    }
}
