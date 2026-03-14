package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.queries.sales.SearchInvoicesQuery;
import com.cybermanager.application.views.sales.InvoiceSummaryView;

import java.util.List;

public interface SearchInvoicesUseCase {
    List<InvoiceSummaryView> execute(SearchInvoicesQuery query);
}
