package com.svc.pokeguessteam.model.user;

import com.svc.pokeguessteam.model.pokemon.EvolutionLineModel;
import com.svc.pokeguessteam.model.pokemon.PokemonModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Inventário por <strong>linha evolutiva</strong>: progresso e obtenções agregados à cadeia,
 * não a uma entrada {@link PokemonModel} isolada.
 */
@Entity
@Table(
        name = "TB_USER_POKEMON_INVENTORY",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_USER_POKEMON_INV_PROFILE_LINE",
                columnNames = {"FK_PROFILE_ID", "FK_EVOLUTION_LINE_ID"}
        )
)
public class UserPokemonInventoryModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_USER_POKEMON_INVENTORY_ID", nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_PROFILE_ID", nullable = false)
    private ProfileModel profile;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "FK_EVOLUTION_LINE_ID", referencedColumnName = "LINE_KEY", nullable = false)
    private EvolutionLineModel evolutionLine;

    @Column(name = "LEVEL", nullable = false)
    private Integer level;

    @Column(name = "TOTAL_XP", nullable = false)
    private Integer totalXp;

    /** Quantas vezes o jogador obteve/capturou espécies desta linha (inclui duplicados). */
    @Column(name = "TIMES_OBTAINED", nullable = false)
    private Integer timesObtained;

    public String getId() {
        return id;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void setProfile(ProfileModel profile) {
        this.profile = profile;
    }

    public EvolutionLineModel getEvolutionLine() {
        return evolutionLine;
    }

    public void setEvolutionLine(EvolutionLineModel evolutionLine) {
        this.evolutionLine = evolutionLine;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(Integer totalXp) {
        this.totalXp = totalXp;
    }

    public Integer getTimesObtained() {
        return timesObtained;
    }

    public void setTimesObtained(Integer timesObtained) {
        this.timesObtained = timesObtained;
    }
}
