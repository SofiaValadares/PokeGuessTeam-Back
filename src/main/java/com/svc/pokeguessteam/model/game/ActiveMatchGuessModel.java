package com.svc.pokeguessteam.model.game;

import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
import com.svc.pokeguessteam.model.enums.MatchPlayerSideConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TB_ACTIVE_MATCH_GUESSES")
public class ActiveMatchGuessModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_ACTIVE_MATCH_GUESS_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_ACTIVE_MATCH_ID", nullable = false)
    private ActiveMatchModel match;

    @Convert(converter = MatchPlayerSideConverter.class)
    @Column(name = "PLAYER_SIDE", nullable = false, length = 10)
    private MatchPlayerSide playerSide;

    @Column(name = "GUESSED_POKEDEX_NUMBER", nullable = false)
    private Integer guessedPokedexNumber;

    @Column(name = "IS_EXACT_MATCH", nullable = false)
    private boolean exactMatch;

    @Column(name = "IS_TIMED_OUT", nullable = false)
    private boolean timedOut;

    @Column(name = "IS_AUTO_SELECTED", nullable = false)
    private boolean autoSelected;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "TB_ACTIVE_MATCH_GUESS_MATCHES",
            joinColumns = @JoinColumn(name = "FK_ACTIVE_MATCH_GUESS_ID")
    )
    @OrderColumn(name = "MATCH_ORDER")
    @Column(name = "POKEDEX_NUMBER", nullable = false)
    private List<Integer> matchedPokedexNumbers = new ArrayList<>();

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public ActiveMatchModel getMatch() {
        return match;
    }

    public void setMatch(ActiveMatchModel match) {
        this.match = match;
    }

    public MatchPlayerSide getPlayerSide() {
        return playerSide;
    }

    public void setPlayerSide(MatchPlayerSide playerSide) {
        this.playerSide = playerSide;
    }

    public Integer getGuessedPokedexNumber() {
        return guessedPokedexNumber;
    }

    public void setGuessedPokedexNumber(Integer guessedPokedexNumber) {
        this.guessedPokedexNumber = guessedPokedexNumber;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    public List<Integer> getMatchedPokedexNumbers() {
        return matchedPokedexNumbers;
    }

    public void setMatchedPokedexNumbers(List<Integer> matchedPokedexNumbers) {
        this.matchedPokedexNumbers = matchedPokedexNumbers != null
                ? new ArrayList<>(matchedPokedexNumbers)
                : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public boolean isAutoSelected() {
        return autoSelected;
    }

    public void setAutoSelected(boolean autoSelected) {
        this.autoSelected = autoSelected;
    }
}
