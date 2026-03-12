package com.cybermanager.infrastructure.security.users;

import java.util.Set;

public record AuthenticatedActor(String email, Set<String> roles) {
}

