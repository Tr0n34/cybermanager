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
        LocalDateTime pausedAt,
        boolean paid,
        int pausedSeconds,
        int consumedSeconds,
        Money calculatedPrice
) {
    public CafeSession {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(customerId, "customerId is required");
        if (workstation == null || workstation.isBlank()) {
            throw new IllegalArgumentException("Workstation is required");
        }
        Objects.requireNonNull(startedAt, "startedAt is required");
        if (pausedSeconds < 0) {
            throw new IllegalArgumentException("Paused seconds cannot be negative");
        }
        if (consumedSeconds < 0) {
            throw new IllegalArgumentException("Consumed seconds cannot be negative");
        }
    }

    public static CafeSession start(CustomerId customerId, String workstation, LocalDateTime startedAt) {
        return new CafeSession(SessionId.newId(), customerId, workstation, startedAt, null, null, false, 0, 0, Money.of("0"));
    }

    public CafeSession pause(LocalDateTime pausedAt) {
        if (!active()) {
            throw new IllegalStateException("Cannot pause an ended session");
        }
        if (paused()) {
            throw new IllegalStateException("Session is already paused");
        }
        return new CafeSession(id, customerId, workstation, startedAt, endedAt, pausedAt, paid, pausedSeconds, consumedSeconds, calculatedPrice);
    }

    public CafeSession resume(LocalDateTime resumedAt) {
        if (!paused()) {
            throw new IllegalStateException("Session is not paused");
        }
        int additionalPausedSeconds = (int) Math.max(0, Duration.between(pausedAt, resumedAt).getSeconds());
        return new CafeSession(id, customerId, workstation, startedAt, endedAt, null, paid, pausedSeconds + additionalPausedSeconds, consumedSeconds, calculatedPrice);
    }

    public CafeSession stop(LocalDateTime endedAt, Money calculatedPrice, boolean paid) {
        return new CafeSession(id, customerId, workstation, startedAt, endedAt, null, paid, pausedSecondsUntil(endedAt), consumedSecondsUntil(endedAt), calculatedPrice);
    }

    public CafeSession markPaid() {
        if (!paid && endedAt == null) {
            throw new IllegalStateException("Cannot mark an active session as paid");
        }
        return new CafeSession(id, customerId, workstation, startedAt, endedAt, null, true, pausedSeconds, consumedSeconds, calculatedPrice);
    }

    public boolean active() {
        return endedAt == null;
    }

    public boolean paused() {
        return pausedAt != null && active();
    }

    public int consumedSecondsUntil(LocalDateTime referenceTime) {
        long totalSeconds = Math.max(0, Duration.between(startedAt, referenceTime).getSeconds());
        return (int) Math.max(0, totalSeconds - pausedSecondsUntil(referenceTime));
    }

    public int consumedMinutesUntil(LocalDateTime referenceTime) {
        long effectiveSeconds = Math.max(0, consumedSecondsUntil(referenceTime));
        return (int) Math.max(1, (effectiveSeconds + 59) / 60);
    }

    private int pausedSecondsUntil(LocalDateTime referenceTime) {
        if (!paused()) {
            return pausedSeconds;
        }
        return pausedSeconds + (int) Math.max(0, Duration.between(pausedAt, referenceTime).getSeconds());
    }
}

