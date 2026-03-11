package com.cybermanager.auth.domain.model;

import java.util.Objects;
import java.util.Set;

public final class UserAccount {
    private final UserId id;
    private final EmailAddress email;
    private final String firstName;
    private final String lastName;
    private final PasswordHash passwordHash;
    private final Set<AppRole> roles;
    private final UserStatus status;

    public UserAccount(
            UserId id,
            EmailAddress email,
            String firstName,
            String lastName,
            PasswordHash passwordHash,
            Set<AppRole> roles,
            UserStatus status
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.email = Objects.requireNonNull(email, "email is required");
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash is required");
        this.roles = Set.copyOf(Objects.requireNonNull(roles, "roles are required"));
        if (this.roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
        this.status = Objects.requireNonNull(status, "status is required");
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public UserId id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Set<AppRole> roles() {
        return roles;
    }

    public UserStatus status() {
        return status;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

