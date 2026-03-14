package com.cybermanager.application.usecases.session;

import com.cybermanager.application.commands.session.PaySessionAndCreateInvoiceCommand;
import com.cybermanager.application.views.sales.InvoicePdfView;

public interface PaySessionAndCreateInvoiceUseCase {
    InvoicePdfView execute(PaySessionAndCreateInvoiceCommand command);
}
