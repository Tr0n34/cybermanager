package com.cybermanager.application.usecases.company;

import com.cybermanager.application.commands.company.UpsertCompanyProfileCommand;
import com.cybermanager.application.views.company.CompanyProfileView;

public interface UpsertCompanyProfileUseCase {
    CompanyProfileView execute(UpsertCompanyProfileCommand command);
}
