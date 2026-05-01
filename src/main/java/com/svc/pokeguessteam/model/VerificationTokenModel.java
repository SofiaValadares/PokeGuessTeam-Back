package com.svc.pokeguessteam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_VERIFICATION_TOKENS")
public class VerificationTokenModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_TOKEN_ID")
    private String id;

    @Column(name = "TOKEN_VALUE", nullable = false, unique = true)
    private String token;

    @Column(name = "TOKEN_EMAIL", nullable = false)
    private String email;

    @Column(name = "TOKEN_EXPIRATION_DATE", nullable = false)
    private LocalDateTime expiryDate;

    public VerificationTokenModel() {}

    public VerificationTokenModel(String token, String email) {
        this.token = token;
        this.email = email;
        this.expiryDate = LocalDateTime.now().plusMinutes(15);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
}