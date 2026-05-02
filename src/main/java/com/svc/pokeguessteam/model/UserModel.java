package com.svc.pokeguessteam.model;

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

    @PrePersist
    protected void onCreate() {
        this.registerDate = LocalDateTime.now();
        this.emailVerify = false;
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
}