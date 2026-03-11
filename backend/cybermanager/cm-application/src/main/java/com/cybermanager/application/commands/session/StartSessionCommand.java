package com.cybermanager.application.commands.session;

import java.util.UUID;

public record StartSessionCommand(UUID customerId, String customerName) {
}
