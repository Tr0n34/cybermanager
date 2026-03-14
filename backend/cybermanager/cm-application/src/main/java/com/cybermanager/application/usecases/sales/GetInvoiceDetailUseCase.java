package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.queries.sales.GetInvoiceDetailQuery;
import com.cybermanager.application.views.sales.InvoiceDetailView;

public interface GetInvoiceDetailUseCase {
    InvoiceDetailView execute(GetInvoiceDetailQuery query);
}
