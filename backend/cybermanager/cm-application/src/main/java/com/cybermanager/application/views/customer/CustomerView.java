package com.cybermanager.application.views.customer;

import java.util.UUID;

public record CustomerView(UUID customerId, String name, String type, String status, int remainingMinutes) {
}
