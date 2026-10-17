package com.cecamed.backend.domain.identity;

import java.time.Instant;
import java.util.Objects;

public class SecurityAuditEvent {
    private final Long id;
    private final AuditEventType eventType;
    private final String username;
    private final String ipAddress;
    private final String userAgent;
    private final Instant occurredAt;
    private final boolean successful;
    private final String details;

    public SecurityAuditEvent(
            Long id,
            AuditEventType eventType,
            String username,
            String ipAddress,
            String userAgent,
            Instant occurredAt,
            boolean successful,
            String details) {
        this.id = id;
        this.eventType = Objects.requireNonNull(eventType, "eventType no puede ser nulo");
        this.username = username != null ? username.trim().toLowerCase() : null;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt no puede ser nulo");
        this.successful = successful;
        this.details = details;
    }

    public static SecurityAuditEvent of(
            AuditEventType eventType,
            String username,
            String ipAddress,
            String userAgent,
            Instant occurredAt,
            boolean successful,
            String details) {
        return new SecurityAuditEvent(
                null,
                eventType,
                username,
                ipAddress,
                userAgent,
                occurredAt,
                successful,
                details
        );
    }

    public Long getId() {
        return id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getDetails() {
        return details;
    }
}
