package com.cecamed.backend.infrastructure.persistence.identity;

import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.UserAccount;
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
@Table(name = "user_accounts", schema = "cecamed")
public class UserAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccountJpaEntity() {}

    public UserAccountJpaEntity(
            Long id,
            String username,
            String passwordHash,
            String fullName,
            Role role,
            boolean active,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserAccountJpaEntity fromDomain(UserAccount domain) {
        return new UserAccountJpaEntity(
                domain.getId(),
                domain.getUsername(),
                domain.getPasswordHash(),
                domain.getFullName(),
                domain.getRole(),
                domain.isActive(),
                domain.getFailedLoginAttempts(),
                domain.getLockedUntil(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public UserAccount toDomain() {
        return new UserAccount(
                this.id,
                this.username,
                this.passwordHash,
                this.fullName,
                this.role,
                this.active,
                this.failedLoginAttempts,
                this.lockedUntil,
                this.createdAt,
                this.updatedAt
        );
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccountJpaEntity that = (UserAccountJpaEntity) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
