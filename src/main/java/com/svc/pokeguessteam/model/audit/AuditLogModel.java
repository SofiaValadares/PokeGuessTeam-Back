package com.svc.pokeguessteam.model.audit;

import com.svc.pokeguessteam.model.enums.AuditLogCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "TB_AUDIT_LOG",
        indexes = {
                @Index(name = "IDX_AUDIT_CATEGORY_CREATED", columnList = "CATEGORY, CREATED_AT"),
                @Index(name = "IDX_AUDIT_ACTOR_CREATED", columnList = "ACTOR_USER_ID, CREATED_AT")
        }
)
public class AuditLogModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_AUDIT_ID")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", nullable = false, length = 32)
    private AuditLogCategory category;

    @Column(name = "ACTOR_USER_ID", length = 36)
    private String actorUserId;

    @Column(name = "ACTOR_USERNAME", length = 100)
    private String actorUsername;

    @Column(name = "ACTOR_EMAIL", length = 255)
    private String actorEmail;

    @Column(name = "HTTP_METHOD", length = 16)
    private String httpMethod;

    @Column(name = "PATH", length = 512)
    private String path;

    @Column(name = "STATUS_CODE")
    private Integer statusCode;

    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "ACTION", nullable = false, length = 64)
    private String action;

    @Column(name = "DETAIL", length = 2000)
    private String detail;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public AuditLogCategory getCategory() {
        return category;
    }

    public void setCategory(AuditLogCategory category) {
        this.category = category;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
