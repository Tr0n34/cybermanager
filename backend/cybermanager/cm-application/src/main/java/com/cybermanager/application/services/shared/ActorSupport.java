package com.cybermanager.application.services.shared;

import com.cybermanager.domain.model.users.AppRole;

import java.util.Set;

public final class ActorSupport {
    private ActorSupport() {
    }

    public static void requireAdmin(Set<String> roles) {
        if (roles == null || roles.stream().noneMatch(role -> AppRole.ADMIN.name().equalsIgnoreCase(role))) {
            throw new BusinessException(BusinessErrorType.FORBIDDEN, "ADMIN_ROLE_REQUIRED", "Administrator role is required");
        }
    }
}

