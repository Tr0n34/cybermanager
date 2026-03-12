package com.cybermanager.auth.application.commands;

public record AuthenticateUserCommand(String email, String password) {
}

