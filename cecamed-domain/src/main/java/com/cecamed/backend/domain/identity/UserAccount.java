package com.cecamed.backend.domain.identity;

import java.time.Instant;
import java.util.Objects;

public class UserAccount {
    private final Long id;
    private final String username;
    private String passwordHash;
    private String fullName;
    private Role role;
    private boolean active;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserAccount(
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
        this.username = Objects.requireNonNull(username, "username no puede ser nulo").trim().toLowerCase();
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash no puede ser nulo");
        this.fullName = Objects.requireNonNull(fullName, "fullName no puede ser nulo").trim();
        this.role = Objects.requireNonNull(role, "role no puede ser nulo");
        this.active = active;
        this.failedLoginAttempts = Math.max(0, failedLoginAttempts);
        this.lockedUntil = lockedUntil;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt no puede ser nulo");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt no puede ser nulo");
    }

    public static UserAccount createNew(
            String username,
            String passwordHash,
            String fullName,
            Role role,
            Instant now) {
        return new UserAccount(
                null,
                username,
                passwordHash,
                fullName,
                role,
                true,
                0,
                null,
                now,
                now
        );
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerFailedLogin(Instant now, int maxAttempts, int lockoutDurationMinutes) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = now.plusSeconds(lockoutDurationMinutes * 60L);
        }
        this.updatedAt = now;
    }

    public void registerSuccessfulLogin(Instant now) {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    public void changePassword(String newPasswordHash, Instant now) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash no puede ser nulo");
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    public void updateDetails(String newFullName, Role newRole, Instant now) {
        this.fullName = Objects.requireNonNull(newFullName, "newFullName no puede ser nulo").trim();
        this.role = Objects.requireNonNull(newRole, "newRole no puede ser nulo");
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        this.active = true;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccount that = (UserAccount) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
