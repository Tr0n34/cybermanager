package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.commands.sales.CancelInvoiceCommand;
import com.cybermanager.application.views.sales.InvoiceDetailView;

public interface CancelInvoiceUseCase {
    InvoiceDetailView execute(CancelInvoiceCommand command);
}
