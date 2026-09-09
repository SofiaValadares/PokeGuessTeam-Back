package com.svc.pokeguessteam.model.admin;

import com.svc.pokeguessteam.model.enums.BonusEventStatus;
import com.svc.pokeguessteam.model.user.UserModel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TB_BONUS_EVENTS")
public class BonusEventModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_BONUS_EVENT_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "EVENT_NAME", nullable = false, length = 120)
    private String name;

    @Column(name = "EVENT_DESCRIPTION", nullable = false, length = 2000)
    private String description;

    /** Duração após o start (horas). */
    @Column(name = "DURATION_HOURS", nullable = false)
    private Integer durationHours;

    @Column(name = "XP_MULTIPLIER", nullable = false)
    private Double xpMultiplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_STATUS", nullable = false, length = 20)
    private BonusEventStatus status = BonusEventStatus.DRAFT;

    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;

    @Column(name = "ENDS_AT")
    private LocalDateTime endsAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_CREATED_BY_USER_ID")
    private UserModel createdBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "TB_BONUS_EVENT_POKEMON",
            joinColumns = @JoinColumn(name = "FK_BONUS_EVENT_ID")
    )
    @Column(name = "POKEDEX_NUMBER", nullable = false)
    private List<Integer> pokedexNumbers = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = BonusEventStatus.DRAFT;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public Double getXpMultiplier() {
        return xpMultiplier;
    }

    public void setXpMultiplier(Double xpMultiplier) {
        this.xpMultiplier = xpMultiplier;
    }

    public BonusEventStatus getStatus() {
        return status;
    }

    public void setStatus(BonusEventStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public UserModel getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserModel createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Integer> getPokedexNumbers() {
        return pokedexNumbers;
    }

    public void setPokedexNumbers(List<Integer> pokedexNumbers) {
        this.pokedexNumbers = pokedexNumbers != null ? new ArrayList<>(pokedexNumbers) : new ArrayList<>();
    }
}
