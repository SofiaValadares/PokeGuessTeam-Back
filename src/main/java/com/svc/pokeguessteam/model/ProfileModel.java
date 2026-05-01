package com.svc.pokeguessteam.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_PROFILES")
public class ProfileModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_PROFILE_ID")
    private String id;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "FK_USER_ID", nullable = false, unique = true)
    private UserModel user;

    @Column(name = "RANK_TITLE", nullable = false, length = 50)
    private String rankTitle;

    @Column(name = "POKEBALL_COUNT", nullable = false)
    private Integer pokeballCount;

    @Column(name = "GREAT_BALL_COUNT", nullable = false)
    private Integer greatBallCount;

    @Column(name = "ULTRA_BALL_COUNT", nullable = false)
    private Integer ultraBallCount;

    @Column(name = "MASTER_BALL_COUNT", nullable = false)
    private Integer masterBallCount;

    @Column(name = "SHARD_COUNT", nullable = false)
    private Integer shardCount;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (rankTitle == null) {
            rankTitle = "Iniciante";
        }
        if (pokeballCount == null) {
            pokeballCount = 10;
        }
        if (greatBallCount == null) {
            greatBallCount = 0;
        }
        if (ultraBallCount == null) {
            ultraBallCount = 0;
        }
        if (masterBallCount == null) {
            masterBallCount = 0;
        }
        if (shardCount == null) {
            shardCount = 0;
        }
    }

    public String getId() {
        return id;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public String getRankTitle() {
        return rankTitle;
    }

    public void setRankTitle(String rankTitle) {
        this.rankTitle = rankTitle;
    }

    public Integer getPokeballCount() {
        return pokeballCount;
    }

    public void setPokeballCount(Integer pokeballCount) {
        this.pokeballCount = pokeballCount;
    }

    public Integer getGreatBallCount() {
        return greatBallCount;
    }

    public void setGreatBallCount(Integer greatBallCount) {
        this.greatBallCount = greatBallCount;
    }

    public Integer getUltraBallCount() {
        return ultraBallCount;
    }

    public void setUltraBallCount(Integer ultraBallCount) {
        this.ultraBallCount = ultraBallCount;
    }

    public Integer getMasterBallCount() {
        return masterBallCount;
    }

    public void setMasterBallCount(Integer masterBallCount) {
        this.masterBallCount = masterBallCount;
    }

    public Integer getShardCount() {
        return shardCount;
    }

    public void setShardCount(Integer shardCount) {
        this.shardCount = shardCount;
    }
}
