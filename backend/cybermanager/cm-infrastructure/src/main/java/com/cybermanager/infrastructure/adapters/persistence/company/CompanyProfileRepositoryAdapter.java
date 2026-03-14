package com.cybermanager.infrastructure.adapters.persistence.company;

import com.cybermanager.domain.model.company.CompanyProfile;
import com.cybermanager.domain.port.company.CompanyProfileRepository;
import com.cybermanager.infrastructure.entities.persistence.company.CompanyProfileJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.company.CompanyProfileJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CompanyProfileRepositoryAdapter implements CompanyProfileRepository {
    private final CompanyProfileJpaRepository repository;

    public CompanyProfileRepositoryAdapter(CompanyProfileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CompanyProfile> findCurrent() {
        return repository.findTopByOrderByUpdatedAtDesc().map(this::toDomain);
    }

    @Override
    public CompanyProfile save(CompanyProfile companyProfile) {
        CompanyProfileJpaEntity entity = new CompanyProfileJpaEntity();
        entity.id = companyProfile.id();
        entity.legalName = companyProfile.legalName();
        entity.siret = companyProfile.siret();
        entity.phone = companyProfile.phone();
        entity.email = companyProfile.email();
        entity.addressLine1 = companyProfile.addressLine1();
        entity.addressLine2 = companyProfile.addressLine2();
        entity.postalCode = companyProfile.postalCode();
        entity.city = companyProfile.city();
        entity.country = companyProfile.country();
        entity.updatedAt = companyProfile.updatedAt();
        return toDomain(repository.save(entity));
    }

    private CompanyProfile toDomain(CompanyProfileJpaEntity entity) {
        return new CompanyProfile(
                entity.id,
                entity.legalName,
                entity.siret,
                entity.phone,
                entity.email,
                entity.addressLine1,
                entity.addressLine2,
                entity.postalCode,
                entity.city,
                entity.country,
                entity.updatedAt
        );
    }
}
