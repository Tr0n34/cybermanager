package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.commands.sales.CreateManualInvoiceCommand;
import com.cybermanager.application.views.sales.InvoicePdfView;

public interface CreateManualInvoiceUseCase {
    InvoicePdfView execute(CreateManualInvoiceCommand command);
}
