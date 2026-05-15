package com.njxs.lims.archive.persistence;

import com.njxs.lims.archive.domain.ArchiveItemRecord;
import java.util.List;
import java.util.UUID;

public interface ArchiveItemRepository {

  ArchiveItemRecord save(ArchiveItemRecord record);

  List<ArchiveItemRecord> list(UUID projectId, int limit, int offset);

  int count(UUID projectId);

  boolean existsProject(UUID projectId);

  boolean existsFile(UUID fileId);
}
