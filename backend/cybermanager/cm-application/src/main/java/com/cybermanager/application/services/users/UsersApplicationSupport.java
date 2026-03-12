package com.cybermanager.application.services.users;

import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.domain.model.users.AppRole;
import com.cybermanager.domain.model.users.EmailAddress;
import com.cybermanager.domain.model.users.PasswordHash;
import com.cybermanager.domain.model.users.User;
import com.cybermanager.domain.model.users.UserId;
import com.cybermanager.domain.model.users.UserStatus;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class UsersApplicationSupport {
    private UsersApplicationSupport() {
    }

    static void requireAdmin(Set<String> roles) {
        if (roles == null || roles.stream().noneMatch(role -> AppRole.ADMIN.name().equalsIgnoreCase(role))) {
            throw new UserManagementException(BusinessErrorType.FORBIDDEN, "ADMIN_ROLE_REQUIRED", "Administrator role is required");
        }
    }

    static Set<AppRole> mapRoles(Set<String> roles) {
        Objects.requireNonNull(roles, "roles are required");
        if (roles.isEmpty()) {
            throw new UserManagementException(BusinessErrorType.VALIDATION, "USER_ROLE_REQUIRED", "At least one role is required");
        }
        return roles.stream()
                .map(role -> AppRole.valueOf(role.toUpperCase()))
                .collect(Collectors.toSet());
    }

    static UserId userId(UUID value) {
        return new UserId(value);
    }

    static EmailAddress email(String value) {
        return new EmailAddress(value);
    }

    static PasswordHash passwordHash(String value) {
        return new PasswordHash(value);
    }

    static UserStatus status(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UserStatus.valueOf(value.toUpperCase());
    }

    static com.cybermanager.application.views.users.UserDetailsView toDetails(User user) {
        return new com.cybermanager.application.views.users.UserDetailsView(
                user.id().value(),
                user.email().value(),
                user.firstName(),
                user.lastName(),
                user.roles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.status().name()
        );
    }

    static com.cybermanager.application.views.users.UserSummaryView toSummary(User user) {
        return new com.cybermanager.application.views.users.UserSummaryView(
                user.id().value(),
                user.email().value(),
                user.firstName(),
                user.lastName(),
                user.roles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.status().name()
        );
    }
}

