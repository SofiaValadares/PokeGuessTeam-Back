package com.svc.pokeguessteam.model;

import com.svc.pokeguessteam.model.enums.MatchMode;
import com.svc.pokeguessteam.model.enums.MatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_MATCHES")
public class MatchModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_MATCH_ID")
    private String id;

    @Column(name = "PLAYER_A_USER_ID", nullable = false)
    private String playerAUserId;

    @Column(name = "PLAYER_B_USER_ID", nullable = false)
    private String playerBUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "MATCH_MODE", nullable = false, length = 30)
    private MatchMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "MATCH_STATUS", nullable = false, length = 20)
    private MatchStatus status;

    @Column(name = "CURRENT_TURN_SIDE", nullable = false, length = 1)
    private String currentTurnSide;

    @Column(name = "PLAYER_A_SCORE", nullable = false)
    private Integer playerAScore;

    @Column(name = "PLAYER_B_SCORE", nullable = false)
    private Integer playerBScore;

    @Column(name = "WINNER_USER_ID")
    private String winnerUserId;

    @Column(name = "FORCE_DRAW_ACTIVE", nullable = false)
    private Boolean forceDrawActive;

    @Column(name = "FORCE_DRAW_SIDE", length = 1)
    private String forceDrawSide;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = MatchStatus.IN_PROGRESS;
        }
        if (currentTurnSide == null) {
            currentTurnSide = "A";
        }
        if (playerAScore == null) {
            playerAScore = 0;
        }
        if (playerBScore == null) {
            playerBScore = 0;
        }
        if (forceDrawActive == null) {
            forceDrawActive = false;
        }
    }

    public String getId() {
        return id;
    }

    public String getPlayerAUserId() {
        return playerAUserId;
    }

    public void setPlayerAUserId(String playerAUserId) {
        this.playerAUserId = playerAUserId;
    }

    public String getPlayerBUserId() {
        return playerBUserId;
    }

    public void setPlayerBUserId(String playerBUserId) {
        this.playerBUserId = playerBUserId;
    }

    public MatchMode getMode() {
        return mode;
    }

    public void setMode(MatchMode mode) {
        this.mode = mode;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public String getCurrentTurnSide() {
        return currentTurnSide;
    }

    public void setCurrentTurnSide(String currentTurnSide) {
        this.currentTurnSide = currentTurnSide;
    }

    public Integer getPlayerAScore() {
        return playerAScore;
    }

    public void setPlayerAScore(Integer playerAScore) {
        this.playerAScore = playerAScore;
    }

    public Integer getPlayerBScore() {
        return playerBScore;
    }

    public void setPlayerBScore(Integer playerBScore) {
        this.playerBScore = playerBScore;
    }

    public String getWinnerUserId() {
        return winnerUserId;
    }

    public void setWinnerUserId(String winnerUserId) {
        this.winnerUserId = winnerUserId;
    }

    public Boolean getForceDrawActive() {
        return forceDrawActive;
    }

    public void setForceDrawActive(Boolean forceDrawActive) {
        this.forceDrawActive = forceDrawActive;
    }

    public String getForceDrawSide() {
        return forceDrawSide;
    }

    public void setForceDrawSide(String forceDrawSide) {
        this.forceDrawSide = forceDrawSide;
    }
}
