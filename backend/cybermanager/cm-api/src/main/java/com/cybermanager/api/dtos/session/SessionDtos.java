package com.cybermanager.api.dtos.session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class SessionDtos {
    private SessionDtos() {
    }

    public record StartSessionRequest(UUID customerId, String customerName) {}

    public record SessionResponse(
            UUID sessionId,
            UUID customerId,
            String customerName,
            String customerType,
            int remainingMinutes,
            String workstation,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            int consumedMinutes,
            BigDecimal calculatedPrice
    ) {}

    public record CurrentSessionsResponse(List<SessionResponse> sessions) {}
}
