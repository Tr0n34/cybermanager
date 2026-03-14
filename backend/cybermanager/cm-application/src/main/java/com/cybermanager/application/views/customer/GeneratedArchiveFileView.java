package com.cybermanager.application.views.customer;

import java.time.LocalDateTime;
import java.util.UUID;

public record GeneratedArchiveFileView(
        UUID id,
        String fileName,
        String fileType,
        String generatedBy,
        LocalDateTime generatedAt
) {
}
