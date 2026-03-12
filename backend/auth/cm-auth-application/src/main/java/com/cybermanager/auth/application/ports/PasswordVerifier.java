package com.cybermanager.auth.application.ports;

public interface PasswordVerifier {
    boolean matches(String rawPassword, String passwordHash);
}

