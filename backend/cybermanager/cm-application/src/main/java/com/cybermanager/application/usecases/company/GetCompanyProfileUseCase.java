package com.cybermanager.application.usecases.company;

import com.cybermanager.application.queries.company.GetCompanyProfileQuery;
import com.cybermanager.application.views.company.CompanyProfileView;

public interface GetCompanyProfileUseCase {
    CompanyProfileView execute(GetCompanyProfileQuery query);
}
