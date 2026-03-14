package com.cybermanager.api.dtos.company;

import java.time.LocalDateTime;
import java.util.UUID;

public final class CompanyDtos {
    private CompanyDtos() {
    }

    public record CompanyProfileRequest(
            String legalName,
            String siret,
            String phone,
            String email,
            String addressLine1,
            String addressLine2,
            String postalCode,
            String city,
            String country
    ) {
    }

    public record CompanyProfileResponse(
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
}
