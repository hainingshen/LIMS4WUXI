package com.njxs.lims.file.persistence;

import com.njxs.lims.file.domain.FileMetadata;

public interface FileMetadataRepository {

  void save(FileMetadata fileMetadata);
}
