package com.cybermanager.application.commands.session;

import java.util.UUID;

public record StopSessionCommand(UUID sessionId) {
}
