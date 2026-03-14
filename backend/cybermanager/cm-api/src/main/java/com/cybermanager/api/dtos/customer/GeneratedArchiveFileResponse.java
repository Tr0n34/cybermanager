package com.cybermanager.api.dtos.customer;

import java.time.LocalDateTime;
import java.util.UUID;

public record GeneratedArchiveFileResponse(
        UUID id,
        String fileName,
        String fileType,
        String generatedBy,
        LocalDateTime generatedAt
) {
}
