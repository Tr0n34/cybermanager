package com.cybermanager.domain.port.company;

import com.cybermanager.domain.model.company.CompanyProfile;

import java.util.Optional;

public interface CompanyProfileRepository {
    Optional<CompanyProfile> findCurrent();
    CompanyProfile save(CompanyProfile companyProfile);
}
