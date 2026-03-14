package com.cybermanager.domain.port.customer;

import com.cybermanager.domain.model.customer.GeneratedArchiveFile;

import java.util.List;

public interface GeneratedArchiveFileRepository {
    GeneratedArchiveFile save(GeneratedArchiveFile file);
    List<GeneratedArchiveFile> findRecent();
}
