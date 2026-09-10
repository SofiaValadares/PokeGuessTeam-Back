package com.svc.pokeguessteam.model.user;

import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
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
 * Equipa de treino: 6 slots, cada um com uma {@link EvolutionLineModel} do inventário do jogador (PC).
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
    @JoinColumn(name = "FK_SLOT_1_LINE_KEY", referencedColumnName = "LINE_KEY")
    private EvolutionLineModel slot1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_2_LINE_KEY", referencedColumnName = "LINE_KEY")
    private EvolutionLineModel slot2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_3_LINE_KEY", referencedColumnName = "LINE_KEY")
    private EvolutionLineModel slot3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_4_LINE_KEY", referencedColumnName = "LINE_KEY")
    private EvolutionLineModel slot4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_5_LINE_KEY", referencedColumnName = "LINE_KEY")
    private EvolutionLineModel slot5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SLOT_6_LINE_KEY", referencedColumnName = "LINE_KEY")
    private EvolutionLineModel slot6;

    public String getId() {
        return id;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void setProfile(ProfileModel profile) {
        this.profile = profile;
    }

    public EvolutionLineModel getSlot1() {
        return slot1;
    }

    public void setSlot1(EvolutionLineModel slot1) {
        this.slot1 = slot1;
    }

    public EvolutionLineModel getSlot2() {
        return slot2;
    }

    public void setSlot2(EvolutionLineModel slot2) {
        this.slot2 = slot2;
    }

    public EvolutionLineModel getSlot3() {
        return slot3;
    }

    public void setSlot3(EvolutionLineModel slot3) {
        this.slot3 = slot3;
    }

    public EvolutionLineModel getSlot4() {
        return slot4;
    }

    public void setSlot4(EvolutionLineModel slot4) {
        this.slot4 = slot4;
    }

    public EvolutionLineModel getSlot5() {
        return slot5;
    }

    public void setSlot5(EvolutionLineModel slot5) {
        this.slot5 = slot5;
    }

    public EvolutionLineModel getSlot6() {
        return slot6;
    }

    public void setSlot6(EvolutionLineModel slot6) {
        this.slot6 = slot6;
    }

    /** Índice 0–5 corresponde ao slot 1–6. */
    public EvolutionLineModel getSlot(int indexZeroBased) {
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
    public void setSlot(int indexZeroBased, EvolutionLineModel line) {
        switch (indexZeroBased) {
            case 0 -> setSlot1(line);
            case 1 -> setSlot2(line);
            case 2 -> setSlot3(line);
            case 3 -> setSlot4(line);
            case 4 -> setSlot5(line);
            case 5 -> setSlot6(line);
            default -> throw new IllegalArgumentException("Slot index must be 0..5");
        }
    }
}
