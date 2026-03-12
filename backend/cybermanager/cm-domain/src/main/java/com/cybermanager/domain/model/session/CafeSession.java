package com.cybermanager.domain.model.session;

import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.shared.Money;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record CafeSession(
        SessionId id,
        CustomerId customerId,
        String workstation,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        int consumedMinutes,
        Money calculatedPrice
) {
    public CafeSession {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(customerId, "customerId is required");
        if (workstation == null || workstation.isBlank()) {
            throw new IllegalArgumentException("Workstation is required");
        }
        Objects.requireNonNull(startedAt, "startedAt is required");
        if (consumedMinutes < 0) {
            throw new IllegalArgumentException("Consumed minutes cannot be negative");
        }
    }

    public static CafeSession start(CustomerId customerId, String workstation, LocalDateTime startedAt) {
        return new CafeSession(SessionId.newId(), customerId, workstation, startedAt, null, 0, Money.of("0"));
    }

    public CafeSession stop(LocalDateTime endedAt, Money calculatedPrice) {
        int minutes = (int) Math.max(1, Duration.between(startedAt, endedAt).toMinutes());
        return new CafeSession(id, customerId, workstation, startedAt, endedAt, minutes, calculatedPrice);
    }

    public boolean active() {
        return endedAt == null;
    }
}

