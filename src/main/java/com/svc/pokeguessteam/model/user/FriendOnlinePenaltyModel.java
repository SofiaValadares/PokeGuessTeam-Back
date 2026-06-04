package com.svc.pokeguessteam.model.user;

import com.svc.pokeguessteam.model.game.ActiveMatchModel;
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
@Table(name = "TB_FRIEND_ONLINE_PENALTIES")
public class FriendOnlinePenaltyModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_FRIEND_ONLINE_PENALTY_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_PROFILE_ID", nullable = false)
    private ProfileModel profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_ACTIVE_MATCH_ID")
    private ActiveMatchModel match;

    @Column(name = "OCCURRED_AT", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    public void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void setProfile(ProfileModel profile) {
        this.profile = profile;
    }

    public ActiveMatchModel getMatch() {
        return match;
    }

    public void setMatch(ActiveMatchModel match) {
        this.match = match;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
