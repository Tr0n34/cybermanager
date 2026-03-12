package com.cybermanager.auth.application.ports;

import java.util.UUID;

public interface TokenReader {
    UUID readUserId(String token);
}

