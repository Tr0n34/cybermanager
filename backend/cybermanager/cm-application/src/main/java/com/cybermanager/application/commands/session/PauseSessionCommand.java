package com.cybermanager.application.commands.session;

import java.util.UUID;

public record PauseSessionCommand(UUID sessionId) {
}
