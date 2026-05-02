package com.svc.pokeguessteam.model;

import com.svc.pokeguessteam.model.enums.PokemonRarity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TB_EVOLUTION_LINES")
public class EvolutionLineModel {

    @Id
    @Column(name = "LINE_KEY", nullable = false)
    private Integer lineKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "LINE_RARITY", nullable = false, length = 20)
    private PokemonRarity rarity;

    /**
     * Números nacionais da Pokédex ({@code pokedexNumber}), na ordem do ramo evolutivo.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "TB_EVOLUTION_LINE_MEMBERS",
            joinColumns = @JoinColumn(name = "FK_EVOLUTION_LINE_ID", referencedColumnName = "LINE_KEY")
    )
    
    @Column(name = "MEMBER_POKEDEX_NUMBER", nullable = false)
    private List<Integer> memberPokedexNumbers = new ArrayList<>();

    public Integer getLineKey() {
        return lineKey;
    }

    public void setLineKey(Integer lineKey) {
        this.lineKey = lineKey;
    }

    public PokemonRarity getRarity() {
        return rarity;
    }

    public void setRarity(PokemonRarity rarity) {
        this.rarity = rarity;
    }

    public List<Integer> getMemberPokedexNumbers() {
        return memberPokedexNumbers;
    }

    public void setMemberPokedexNumbers(List<Integer> memberPokedexNumbers) {
        this.memberPokedexNumbers = memberPokedexNumbers != null ? memberPokedexNumbers : new ArrayList<>();
    }
}
