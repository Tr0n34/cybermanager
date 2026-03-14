package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.queries.sales.ReissueInvoicePdfQuery;
import com.cybermanager.application.views.sales.InvoicePdfView;

public interface ReissueInvoicePdfUseCase {
    InvoicePdfView execute(ReissueInvoicePdfQuery query);
}
