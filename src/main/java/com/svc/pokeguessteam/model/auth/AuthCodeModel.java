package com.svc.pokeguessteam.model.auth;

import com.svc.pokeguessteam.model.user.UserModel;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_AUTH_CODES", indexes = {
        @Index(name = "IDX_AUTH_CODE_USER_PURPOSE", columnList = "FK_USER_ID, CODE_PURPOSE")
})
public class AuthCodeModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_AUTH_CODE_ID")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_USER_ID", nullable = false)
    private UserModel user;

    @Enumerated(EnumType.STRING)
    @Column(name = "CODE_PURPOSE", nullable = false, length = 32)
    private AuthCodePurpose purpose;

    @Column(name = "CODE_HASH", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "CONSUMED_AT")
    private LocalDateTime consumedAt;

    @Column(name = "FAILED_ATTEMPTS", nullable = false)
    private int failedAttempts;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public AuthCodePurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(AuthCodePurpose purpose) {
        this.purpose = purpose;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive(LocalDateTime now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }
}
