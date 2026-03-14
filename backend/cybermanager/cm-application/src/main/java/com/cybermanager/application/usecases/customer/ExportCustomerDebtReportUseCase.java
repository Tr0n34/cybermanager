package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.queries.customer.ExportCustomerDebtReportQuery;
import com.cybermanager.application.views.customer.CustomerDebtReportView;

public interface ExportCustomerDebtReportUseCase {
    CustomerDebtReportView execute(ExportCustomerDebtReportQuery query);
}
