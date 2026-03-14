package com.cybermanager.application.services.company;

import com.cybermanager.application.commands.company.UpsertCompanyProfileCommand;
import com.cybermanager.application.queries.company.GetCompanyProfileQuery;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.application.usecases.company.GetCompanyProfileUseCase;
import com.cybermanager.application.usecases.company.UpsertCompanyProfileUseCase;
import com.cybermanager.application.views.company.CompanyProfileView;
import com.cybermanager.domain.model.company.CompanyProfile;
import com.cybermanager.domain.port.company.CompanyProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class CompanyProfileApplicationService implements GetCompanyProfileUseCase, UpsertCompanyProfileUseCase {
    private final CompanyProfileRepository companyProfileRepository;

    public CompanyProfileApplicationService(CompanyProfileRepository companyProfileRepository) {
        this.companyProfileRepository = companyProfileRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public CompanyProfileView execute(GetCompanyProfileQuery query) {
        return companyProfileRepository.findCurrent()
                .map(this::toView)
                .orElse(null);
    }

    @Override
    public CompanyProfileView execute(UpsertCompanyProfileCommand command) {
        String legalName = normalizeRequired(command.legalName(), "COMPANY_LEGAL_NAME_REQUIRED", "Le nom de l entreprise est requis.");
        CompanyProfile current = companyProfileRepository.findCurrent().orElse(null);
        LocalDateTime updatedAt = LocalDateTime.now();
        CompanyProfile saved = companyProfileRepository.save(current == null
                ? CompanyProfile.create(
                        legalName,
                        normalize(command.siret()),
                        normalize(command.phone()),
                        normalize(command.email()),
                        normalize(command.addressLine1()),
                        normalize(command.addressLine2()),
                        normalize(command.postalCode()),
                        normalize(command.city()),
                        normalize(command.country()),
                        updatedAt
                )
                : current.update(
                        legalName,
                        normalize(command.siret()),
                        normalize(command.phone()),
                        normalize(command.email()),
                        normalize(command.addressLine1()),
                        normalize(command.addressLine2()),
                        normalize(command.postalCode()),
                        normalize(command.city()),
                        normalize(command.country()),
                        updatedAt
                ));
        return toView(saved);
    }

    private String normalizeRequired(String value, String code, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(BusinessErrorType.VALIDATION, code, message);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private CompanyProfileView toView(CompanyProfile companyProfile) {
        return new CompanyProfileView(
                companyProfile.id(),
                companyProfile.legalName(),
                companyProfile.siret(),
                companyProfile.phone(),
                companyProfile.email(),
                companyProfile.addressLine1(),
                companyProfile.addressLine2(),
                companyProfile.postalCode(),
                companyProfile.city(),
                companyProfile.country(),
                companyProfile.updatedAt()
        );
    }
}
