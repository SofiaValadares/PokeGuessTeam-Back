package com.svc.pokeguessteam.model.admin;

import com.svc.pokeguessteam.model.enums.UserRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "TB_ADMIN_ROLE_AUDIT",
        indexes = {
                @Index(
                        name = "IDX_ADMIN_ROLE_AUDIT_CREATED_AT",
                        columnList = "CREATED_AT"
                )
        }
)
public class AdminRoleAuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_AUDIT_ID")
    private String id;

    @Column(name = "ACTOR_USER_ID", length = 64)
    private String actorUserId;

    @Column(name = "ACTOR_USERNAME", length = 100)
    private String actorUsername;

    @Column(name = "TARGET_USER_ID", nullable = false, length = 64)
    private String targetUserId;

    @Column(name = "TARGET_USERNAME", nullable = false, length = 100)
    private String targetUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "FROM_ROLE", nullable = false, length = 20)
    private UserRole fromRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "TO_ROLE", nullable = false, length = 20)
    private UserRole toRole;

    @Column(name = "IP_ADDRESS", length = 64)
    private String ipAddress;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AdminRoleAuditModel() {
    }

    public AdminRoleAuditModel(
            String actorUserId,
            String actorUsername,
            String targetUserId,
            String targetUsername,
            UserRole fromRole,
            UserRole toRole,
            String ipAddress
    ) {
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
        this.fromRole = fromRole;
        this.toRole = toRole;
        this.ipAddress = ipAddress;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public UserRole getFromRole() {
        return fromRole;
    }

    public UserRole getToRole() {
        return toRole;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}