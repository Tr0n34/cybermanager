package com.cybermanager.application.views.company;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyProfileView(
        UUID id,
        String legalName,
        String siret,
        String phone,
        String email,
        String addressLine1,
        String addressLine2,
        String postalCode,
        String city,
        String country,
        LocalDateTime updatedAt
) {
}
