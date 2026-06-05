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
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "TB_ACTIVE_MATCH_PLAYERS",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_ACTIVE_MATCH_PLAYER_SIDE",
                columnNames = {"FK_ACTIVE_MATCH_ID", "PLAYER_SIDE"}
        )
)
public class ActiveMatchPlayerModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_ACTIVE_MATCH_PLAYER_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_ACTIVE_MATCH_ID", nullable = false)
    private ActiveMatchModel match;

    @Convert(converter = MatchPlayerSideConverter.class)
    @Column(name = "PLAYER_SIDE", nullable = false, length = 10)
    private MatchPlayerSide side;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "TB_ACTIVE_MATCH_PLAYER_TEAM",
            joinColumns = @JoinColumn(name = "FK_ACTIVE_MATCH_PLAYER_ID")
    )
    @OrderColumn(name = "SLOT_INDEX")
    @Column(name = "POKEDEX_NUMBER", nullable = false)
    private List<Integer> team;

    /** Carregado/gravado via {@link com.svc.pokeguessteam.service.ActiveMatchHitsService}. */
    @Transient
    private Set<Integer> hits;

    @Column(name = "SKIP_TURNS", nullable = false)
    private int skipTurns;

    /** Penalidades por timeout de turno (modo amigo). */
    @Column(name = "TURN_TIMEOUT_PENALTIES", nullable = false)
    private int turnTimeoutPenalties;

    public String getId() {
        return id;
    }

    public ActiveMatchModel getMatch() {
        return match;
    }

    public void setMatch(ActiveMatchModel match) {
        this.match = match;
    }

    public MatchPlayerSide getSide() {
        return side;
    }

    public void setSide(MatchPlayerSide side) {
        this.side = side;
    }

    public List<Integer> getTeam() {
        return team == null ? List.of() : List.copyOf(team);
    }

    public void setTeam(List<Integer> team) {
        this.team = team != null ? new ArrayList<>(team) : new ArrayList<>();
    }

    public Set<Integer> getHits() {
        return hits == null ? Set.of() : Set.copyOf(hits);
    }

    public void loadHits(Set<Integer> loaded) {
        hits = loaded != null ? new HashSet<>(loaded) : new HashSet<>();
    }

    public void addHit(int pokedexNumber) {
        if (hits == null) {
            hits = new HashSet<>();
        }
        hits.add(pokedexNumber);
    }

    public void addHits(Collection<Integer> pokedexNumbers) {
        if (pokedexNumbers == null) {
            return;
        }
        for (Integer dex : pokedexNumbers) {
            if (dex != null) {
                addHit(dex);
            }
        }
    }

    public void setHits(Set<Integer> newHits) {
        hits = newHits != null ? new HashSet<>(newHits) : new HashSet<>();
    }

    public int getSkipTurns() {
        return skipTurns;
    }

    public void setSkipTurns(int skipTurns) {
        this.skipTurns = skipTurns;
    }

    public int getTurnTimeoutPenalties() {
        return turnTimeoutPenalties;
    }

    public void setTurnTimeoutPenalties(int turnTimeoutPenalties) {
        this.turnTimeoutPenalties = turnTimeoutPenalties;
    }
}
