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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Equipa de treino do jogador: <strong>seis posições</strong> fixas (slots 1–6), cada uma referindo uma espécie
 * ({@link PokemonModel} por número nacional). Na criação do perfil são escolhidas 6 espécies aleatórias por defeito.
 */
@Entity
@Table(
        name = "TB_TRAINING_TEAMS",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_TRAINING_TEAM_PROFILE",
                columnNames = {"FK_PROFILE_ID"}
        )
)
public class TrainingTeamModel {

    public static final int TEAM_SIZE = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_TRAINING_TEAM_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_PROFILE_ID", nullable = false, unique = true)
    private ProfileModel profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_1_POKEDEX_NUMBER", referencedColumnName = "POKEDEX_NUMBER")
    private PokemonModel slot1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_2_POKEDEX_NUMBER", referencedColumnName = "POKEDEX_NUMBER")
    private PokemonModel slot2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_3_POKEDEX_NUMBER", referencedColumnName = "POKEDEX_NUMBER")
    private PokemonModel slot3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_4_POKEDEX_NUMBER", referencedColumnName = "POKEDEX_NUMBER")
    private PokemonModel slot4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_5_POKEDEX_NUMBER", referencedColumnName = "POKEDEX_NUMBER")
    private PokemonModel slot5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_6_POKEDEX_NUMBER", referencedColumnName = "POKEDEX_NUMBER")
    private PokemonModel slot6;

    public String getId() {
        return id;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void setProfile(ProfileModel profile) {
        this.profile = profile;
    }

    public PokemonModel getSlot1() {
        return slot1;
    }

    public void setSlot1(PokemonModel slot1) {
        this.slot1 = slot1;
    }

    public PokemonModel getSlot2() {
        return slot2;
    }

    public void setSlot2(PokemonModel slot2) {
        this.slot2 = slot2;
    }

    public PokemonModel getSlot3() {
        return slot3;
    }

    public void setSlot3(PokemonModel slot3) {
        this.slot3 = slot3;
    }

    public PokemonModel getSlot4() {
        return slot4;
    }

    public void setSlot4(PokemonModel slot4) {
        this.slot4 = slot4;
    }

    public PokemonModel getSlot5() {
        return slot5;
    }

    public void setSlot5(PokemonModel slot5) {
        this.slot5 = slot5;
    }

    public PokemonModel getSlot6() {
        return slot6;
    }

    public void setSlot6(PokemonModel slot6) {
        this.slot6 = slot6;
    }

    /** Índice 0–5 corresponde ao slot 1–6. */
    public PokemonModel getSlot(int indexZeroBased) {
        return switch (indexZeroBased) {
            case 0 -> slot1;
            case 1 -> slot2;
            case 2 -> slot3;
            case 3 -> slot4;
            case 4 -> slot5;
            case 5 -> slot6;
            default -> throw new IllegalArgumentException("Slot index must be 0..5");
        };
    }

    /** Índice 0–5 corresponde ao slot 1–6. */
    public void setSlot(int indexZeroBased, PokemonModel pokemon) {
        switch (indexZeroBased) {
            case 0 -> setSlot1(pokemon);
            case 1 -> setSlot2(pokemon);
            case 2 -> setSlot3(pokemon);
            case 3 -> setSlot4(pokemon);
            case 4 -> setSlot5(pokemon);
            case 5 -> setSlot6(pokemon);
            default -> throw new IllegalArgumentException("Slot index must be 0..5");
        }
    }
}
