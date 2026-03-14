package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.queries.customer.SearchCustomerArchiveCandidatesQuery;
import com.cybermanager.application.views.customer.CustomerArchiveCandidateView;

import java.util.List;

public interface SearchCustomerArchiveCandidatesUseCase {
    List<CustomerArchiveCandidateView> execute(SearchCustomerArchiveCandidatesQuery query);
}
