package com.cybermanager.domain.model.company;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyProfile(
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
    public static CompanyProfile create(
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
        return new CompanyProfile(UUID.randomUUID(), legalName, siret, phone, email, addressLine1, addressLine2, postalCode, city, country, updatedAt);
    }

    public CompanyProfile update(
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
        return new CompanyProfile(id, legalName, siret, phone, email, addressLine1, addressLine2, postalCode, city, country, updatedAt);
    }
}
