package com.cybermanager.domain.model.customer;

import java.time.LocalDateTime;
import java.util.UUID;

public record GeneratedArchiveFile(
        UUID id,
        String fileName,
        String fileType,
        String generatedBy,
        LocalDateTime generatedAt
) {
    public static GeneratedArchiveFile create(String fileName, String fileType, String generatedBy, LocalDateTime generatedAt) {
        return new GeneratedArchiveFile(UUID.randomUUID(), fileName, fileType, generatedBy, generatedAt);
    }
}
