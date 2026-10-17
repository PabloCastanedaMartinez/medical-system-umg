package com.cecamed.backend.infrastructure.persistence.identity;

import com.cecamed.backend.domain.identity.AuditEventType;
import com.cecamed.backend.domain.identity.SecurityAuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "security_audit_logs", schema = "cecamed")
public class SecurityAuditJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(length = 80)
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private boolean successful;

    @Column(length = 500)
    private String details;

    protected SecurityAuditJpaEntity() {}

    public SecurityAuditJpaEntity(
            Long id,
            AuditEventType eventType,
            String username,
            String ipAddress,
            String userAgent,
            Instant occurredAt,
            boolean successful,
            String details) {
        this.id = id;
        this.eventType = eventType;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.occurredAt = occurredAt;
        this.successful = successful;
        this.details = details;
    }

    public static SecurityAuditJpaEntity fromDomain(SecurityAuditEvent domain) {
        return new SecurityAuditJpaEntity(
                domain.getId(),
                domain.getEventType(),
                domain.getUsername(),
                domain.getIpAddress(),
                domain.getUserAgent(),
                domain.getOccurredAt(),
                domain.isSuccessful(),
                domain.getDetails()
        );
    }

    public SecurityAuditEvent toDomain() {
        return new SecurityAuditEvent(
                this.id,
                this.eventType,
                this.username,
                this.ipAddress,
                this.userAgent,
                this.occurredAt,
                this.successful,
                this.details
        );
    }

    public Long getId() { return id; }
    public AuditEventType getEventType() { return eventType; }
    public String getUsername() { return username; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getOccurredAt() { return occurredAt; }
    public boolean isSuccessful() { return successful; }
    public String getDetails() { return details; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityAuditJpaEntity that = (SecurityAuditJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
