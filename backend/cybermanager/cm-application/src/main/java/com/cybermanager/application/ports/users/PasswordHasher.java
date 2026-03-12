package com.cybermanager.application.ports.users;

public interface PasswordHasher {
    String hash(String rawPassword);
}

