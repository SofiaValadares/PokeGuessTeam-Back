package com.svc.pokeguessteam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_MATCH_GUESS_LOGS")
public class MatchGuessLogModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_GUESS_LOG_ID")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_MATCH_ID", nullable = false)
    private MatchModel match;

    @Column(name = "GUESSER_SIDE", nullable = false, length = 1)
    private String guesserSide;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "FK_GUESS_POKEMON_ID", nullable = false)
    private PokemonModel guessedPokemon;

    @Column(name = "HIT_SLOT_COUNT", nullable = false)
    private Integer hitSlotCount;

    @Column(name = "HIT_SLOT_INDEXES", nullable = false, length = 100)
    private String hitSlotIndexes;

    @Column(name = "MATCHED_GENERATION_SLOTS", nullable = false, length = 100)
    private String matchedGenerationSlots;

    @Column(name = "MATCHED_TYPE_SLOTS", nullable = false, length = 100)
    private String matchedTypeSlots;

    @Column(name = "MATCHED_COLOR_SLOTS", nullable = false, length = 100)
    private String matchedColorSlots;

    @Column(name = "MATCHED_HEIGHT_SLOTS", nullable = false, length = 100)
    private String matchedHeightSlots;

    @Column(name = "MATCHED_WEIGHT_SLOTS", nullable = false, length = 100)
    private String matchedWeightSlots;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public MatchModel getMatch() {
        return match;
    }

    public void setMatch(MatchModel match) {
        this.match = match;
    }

    public String getGuesserSide() {
        return guesserSide;
    }

    public void setGuesserSide(String guesserSide) {
        this.guesserSide = guesserSide;
    }

    public PokemonModel getGuessedPokemon() {
        return guessedPokemon;
    }

    public void setGuessedPokemon(PokemonModel guessedPokemon) {
        this.guessedPokemon = guessedPokemon;
    }

    public Integer getHitSlotCount() {
        return hitSlotCount;
    }

    public void setHitSlotCount(Integer hitSlotCount) {
        this.hitSlotCount = hitSlotCount;
    }

    public String getHitSlotIndexes() {
        return hitSlotIndexes;
    }

    public void setHitSlotIndexes(String hitSlotIndexes) {
        this.hitSlotIndexes = hitSlotIndexes;
    }

    public String getMatchedGenerationSlots() {
        return matchedGenerationSlots;
    }

    public void setMatchedGenerationSlots(String matchedGenerationSlots) {
        this.matchedGenerationSlots = matchedGenerationSlots;
    }

    public String getMatchedTypeSlots() {
        return matchedTypeSlots;
    }

    public void setMatchedTypeSlots(String matchedTypeSlots) {
        this.matchedTypeSlots = matchedTypeSlots;
    }

    public String getMatchedColorSlots() {
        return matchedColorSlots;
    }

    public void setMatchedColorSlots(String matchedColorSlots) {
        this.matchedColorSlots = matchedColorSlots;
    }

    public String getMatchedHeightSlots() {
        return matchedHeightSlots;
    }

    public void setMatchedHeightSlots(String matchedHeightSlots) {
        this.matchedHeightSlots = matchedHeightSlots;
    }

    public String getMatchedWeightSlots() {
        return matchedWeightSlots;
    }

    public void setMatchedWeightSlots(String matchedWeightSlots) {
        this.matchedWeightSlots = matchedWeightSlots;
    }
}
