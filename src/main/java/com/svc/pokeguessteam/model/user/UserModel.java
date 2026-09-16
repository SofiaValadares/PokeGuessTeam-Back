package com.svc.pokeguessteam.model.user;

import com.svc.pokeguessteam.model.enums.UserRole;
import org.hibernate.annotations.ColumnDefault;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_USERS")
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_USER_ID")
    private String idUser;

    @Column(name = "USER_NAME", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "USER_EMAIL", nullable = false, unique = true)
    private String email;

    @Column(name = "USER_EMAIL_VERIFY", nullable = false)
    private Boolean emailVerify;

    @Column(name = "USER_PASSWORD_HASH", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "USER_REGISTER_DATE", nullable = false, updatable = false)
    private LocalDateTime registerDate;

    /** {@code false} até ao primeiro login com sessão criada. */
    @Column(name = "USER_HAS_LOGGED_IN", nullable = false)
    @ColumnDefault("false")
    private Boolean hasLoggedIn = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_ROLE", nullable = false, length = 20)
    @ColumnDefault("'USER'")
    private UserRole role = UserRole.USER;

    @Column(name = "SITE_BANNED_PERMANENT", nullable = false)
    @ColumnDefault("false")
    private Boolean siteBannedPermanent = false;

    @Column(name = "SITE_BANNED_UNTIL")
    private LocalDateTime siteBannedUntil;

    @Column(name = "ONLINE_BANNED_PERMANENT", nullable = false)
    @ColumnDefault("false")
    private Boolean onlineBannedPermanent = false;

    @Column(name = "ONLINE_BANNED_UNTIL")
    private LocalDateTime onlineBannedUntil;

    @Column(name = "BAN_REASON", length = 500)
    private String banReason;

    @PrePersist
    protected void onCreate() {
        this.registerDate = LocalDateTime.now();
        this.emailVerify = false;
        if (this.hasLoggedIn == null) {
            this.hasLoggedIn = false;
        }
        if (this.role == null) {
            this.role = UserRole.USER;
        }
        if (this.siteBannedPermanent == null) {
            this.siteBannedPermanent = false;
        }
        if (this.onlineBannedPermanent == null) {
            this.onlineBannedPermanent = false;
        }
    }

    public String getIdUser() {
        return idUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.emailVerify = false;
    }

    public Boolean getEmailVerify() {
        return emailVerify;
    }

    public void setEmailVerifyTrue() {
        this.emailVerify = true;
    }

    public LocalDateTime getRegisterDate() {
        return registerDate;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean getHasLoggedIn() {
        return hasLoggedIn;
    }

    public void setHasLoggedIn(Boolean hasLoggedIn) {
        this.hasLoggedIn = hasLoggedIn;
    }

    public UserRole getRole() {
        return role != null ? role : UserRole.USER;
    }

    public void setRole(UserRole role) {
        this.role = role != null ? role : UserRole.USER;
    }

    public Boolean getSiteBannedPermanent() {
        return Boolean.TRUE.equals(siteBannedPermanent);
    }

    public void setSiteBannedPermanent(Boolean siteBannedPermanent) {
        this.siteBannedPermanent = Boolean.TRUE.equals(siteBannedPermanent);
    }

    public LocalDateTime getSiteBannedUntil() {
        return siteBannedUntil;
    }

    public void setSiteBannedUntil(LocalDateTime siteBannedUntil) {
        this.siteBannedUntil = siteBannedUntil;
    }

    public Boolean getOnlineBannedPermanent() {
        return Boolean.TRUE.equals(onlineBannedPermanent);
    }

    public void setOnlineBannedPermanent(Boolean onlineBannedPermanent) {
        this.onlineBannedPermanent = Boolean.TRUE.equals(onlineBannedPermanent);
    }

    public LocalDateTime getOnlineBannedUntil() {
        return onlineBannedUntil;
    }

    public void setOnlineBannedUntil(LocalDateTime onlineBannedUntil) {
        this.onlineBannedUntil = onlineBannedUntil;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public boolean isSiteBannedNow() {
        if (Boolean.TRUE.equals(siteBannedPermanent)) {
            return true;
        }
        return siteBannedUntil != null && siteBannedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isOnlineBannedNow() {
        if (Boolean.TRUE.equals(onlineBannedPermanent)) {
            return true;
        }
        return onlineBannedUntil != null && onlineBannedUntil.isAfter(LocalDateTime.now());
    }
}
