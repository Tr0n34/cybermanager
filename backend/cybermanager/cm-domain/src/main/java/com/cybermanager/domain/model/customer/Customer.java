package com.cybermanager.domain.model.customer;

import java.util.Objects;

public record Customer(
        CustomerId id,
        String name,
        CustomerType type,
        CustomerStatus status,
        int remainingMinutes
) {
    public Customer {
        Objects.requireNonNull(id, "id is required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(status, "status is required");
        if (remainingMinutes < 0) {
            throw new IllegalArgumentException("Remaining minutes cannot be negative");
        }
    }

    public static Customer createWalkIn(String name) {
        return new Customer(CustomerId.newId(), name.trim(), CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
    }

    public Customer updateName(String name) {
        return new Customer(id, name, type, status, remainingMinutes);
    }

    public Customer convertToSubscriber(int creditedMinutes) {
        return new Customer(id, name, CustomerType.SUBSCRIBER, status, creditedMinutes);
    }

    public Customer deductMinutes(int consumedMinutes) {
        return new Customer(id, name, type, status, Math.max(0, remainingMinutes - consumedMinutes));
    }

    public Customer addSubscriptionMinutes(int creditedMinutes) {
        int nextMinutes = remainingMinutes + Math.max(0, creditedMinutes);
        return new Customer(id, name, CustomerType.SUBSCRIBER, status, nextMinutes);
    }

    public Customer removeSubscriptionMinutes(int minutesToRemove) {
        int nextMinutes = Math.max(0, remainingMinutes - Math.max(0, minutesToRemove));
        return new Customer(id, name, CustomerType.SUBSCRIBER, status, nextMinutes);
    }

    public Customer withRemainingMinutes(int nextRemainingMinutes) {
        return new Customer(id, name, type, status, Math.max(0, nextRemainingMinutes));
    }

}

