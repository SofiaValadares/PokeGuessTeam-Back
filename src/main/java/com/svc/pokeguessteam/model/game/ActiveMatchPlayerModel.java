package com.svc.pokeguessteam.model.game;

import com.svc.pokeguessteam.model.enums.MatchPlayerSide;
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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "PLAYER_SIDE", nullable = false, length = 10)
    private MatchPlayerSide side;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "TB_ACTIVE_MATCH_PLAYER_TEAM",
            joinColumns = @JoinColumn(name = "FK_ACTIVE_MATCH_PLAYER_ID")
    )
    @OrderColumn(name = "SLOT_INDEX")
    @Column(name = "POKEDEX_NUMBER", nullable = false)
    private List<Integer> team = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "TB_ACTIVE_MATCH_PLAYER_HITS",
            joinColumns = @JoinColumn(name = "FK_ACTIVE_MATCH_PLAYER_ID")
    )
    @OrderColumn(name = "HIT_ORDER")
    @Column(name = "POKEDEX_NUMBER", nullable = false)
    private List<Integer> hits = new ArrayList<>();

    @Column(name = "SKIP_TURNS", nullable = false)
    private int skipTurns;

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
        return team;
    }

    public void setTeam(List<Integer> team) {
        this.team = team != null ? new ArrayList<>(team) : new ArrayList<>();
    }

    public List<Integer> getHits() {
        return hits;
    }

    public void setHits(List<Integer> hits) {
        this.hits = hits != null ? new ArrayList<>(hits) : new ArrayList<>();
    }

    public int getSkipTurns() {
        return skipTurns;
    }

    public void setSkipTurns(int skipTurns) {
        this.skipTurns = skipTurns;
    }
}
