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
import org.hibernate.annotations.ColumnDefault;

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

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPokedexModel> pokedexEntries = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileInventoryItemModel> inventoryItems = new ArrayList<>();

    @OneToOne(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private TrainingTeamModel trainingTeam;

    /**
     * Fragmentos só da Poké Bola: a cada 10, converte-se numa Poké Bola (ver {@code ProfileService}).
     */
    @Column(name = "POKEBALL_FRAGMENTS", nullable = false)
    @ColumnDefault("0")
    private Integer pokeballFragments = 0;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (pokeballFragments == null) {
            pokeballFragments = 0;
        }
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

    public List<UserPokedexModel> getPokedexEntries() {
        return pokedexEntries;
    }

    public void setPokedexEntries(List<UserPokedexModel> pokedexEntries) {
        this.pokedexEntries = pokedexEntries != null ? pokedexEntries : new ArrayList<>();
    }

    public List<ProfileInventoryItemModel> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<ProfileInventoryItemModel> inventoryItems) {
        this.inventoryItems = inventoryItems != null ? inventoryItems : new ArrayList<>();
    }

    public TrainingTeamModel getTrainingTeam() {
        return trainingTeam;
    }

    public void setTrainingTeam(TrainingTeamModel trainingTeam) {
        this.trainingTeam = trainingTeam;
    }

    public Integer getPokeballFragments() {
        return pokeballFragments;
    }

    public void setPokeballFragments(Integer pokeballFragments) {
        this.pokeballFragments = pokeballFragments != null ? pokeballFragments : 0;
    }
}
