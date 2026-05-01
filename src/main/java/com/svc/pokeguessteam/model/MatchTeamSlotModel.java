package com.svc.pokeguessteam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_MATCH_TEAM_SLOTS")
public class MatchTeamSlotModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_MATCH_SLOT_ID")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_MATCH_ID", nullable = false)
    private MatchModel match;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "FK_POKEMON_ID", nullable = false)
    private PokemonModel pokemon;

    @Column(name = "OWNER_SIDE", nullable = false, length = 1)
    private String ownerSide;

    @Column(name = "SLOT_INDEX", nullable = false)
    private Integer slotIndex;

    @Column(name = "DISCOVERED_BY_OPPONENT", nullable = false)
    private Boolean discoveredByOpponent;

    public String getId() {
        return id;
    }

    public MatchModel getMatch() {
        return match;
    }

    public void setMatch(MatchModel match) {
        this.match = match;
    }

    public PokemonModel getPokemon() {
        return pokemon;
    }

    public void setPokemon(PokemonModel pokemon) {
        this.pokemon = pokemon;
    }

    public String getOwnerSide() {
        return ownerSide;
    }

    public void setOwnerSide(String ownerSide) {
        this.ownerSide = ownerSide;
    }

    public Integer getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(Integer slotIndex) {
        this.slotIndex = slotIndex;
    }

    public Boolean getDiscoveredByOpponent() {
        return discoveredByOpponent;
    }

    public void setDiscoveredByOpponent(Boolean discoveredByOpponent) {
        this.discoveredByOpponent = discoveredByOpponent;
    }
}
