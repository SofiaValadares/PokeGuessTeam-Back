package com.svc.pokeguessteam.model.securitylab;

import com.svc.pokeguessteam.model.enums.SecurityLabEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "TB_SECURITY_LAB_EVENT",
        indexes = {
                @Index(name = "IDX_SEC_LAB_TYPE_CREATED", columnList = "EVENT_TYPE, CREATED_AT"),
                @Index(name = "IDX_SEC_LAB_CREATED", columnList = "CREATED_AT")
        }
)
public class SecurityLabEventModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_SEC_LAB_EVENT_ID")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 32)
    private SecurityLabEventType eventType;

    @Column(name = "ACTOR_USER_ID", length = 36)
    private String actorUserId;

    @Column(name = "ACTOR_USERNAME", length = 100)
    private String actorUsername;

    @Column(name = "ACTOR_WAS_DEVELOPER", nullable = false)
    private Boolean actorWasDeveloper = false;

    @Column(name = "SUMMARY", nullable = false, length = 255)
    private String summary;

    @Lob
    @Column(name = "DETAIL_JSON")
    private String detailJson;

    @Column(name = "METRIC_VALUE")
    private Double metricValue;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (actorWasDeveloper == null) {
            actorWasDeveloper = false;
        }
    }

    public String getId() {
        return id;
    }

    public SecurityLabEventType getEventType() {
        return eventType;
    }

    public void setEventType(SecurityLabEventType eventType) {
        this.eventType = eventType;
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

    public boolean isActorWasDeveloper() {
        return Boolean.TRUE.equals(actorWasDeveloper);
    }

    public void setActorWasDeveloper(Boolean actorWasDeveloper) {
        this.actorWasDeveloper = Boolean.TRUE.equals(actorWasDeveloper);
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public Double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(Double metricValue) {
        this.metricValue = metricValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
