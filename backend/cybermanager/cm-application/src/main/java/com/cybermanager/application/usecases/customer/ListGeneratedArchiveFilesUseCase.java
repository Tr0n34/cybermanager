package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.queries.customer.ListGeneratedArchiveFilesQuery;
import com.cybermanager.application.views.customer.GeneratedArchiveFileView;

import java.util.List;

public interface ListGeneratedArchiveFilesUseCase {
    List<GeneratedArchiveFileView> execute(ListGeneratedArchiveFilesQuery query);
}
