package com.njxs.lims.archive.persistence;

import com.njxs.lims.archive.domain.ArchiveItemRecord;
import com.njxs.lims.permission.InternalDataScope;
import java.util.List;
import java.util.UUID;

public interface ArchiveItemRepository {

  ArchiveItemRecord save(ArchiveItemRecord record);

  List<ArchiveItemRecord> list(UUID projectId, int limit, int offset);

  int count(UUID projectId);

  List<ArchiveItemRecord> listVisibleTo(InternalDataScope scope, UUID projectId, int limit, int offset);

  int countVisibleTo(InternalDataScope scope, UUID projectId);

  boolean existsProject(UUID projectId);

  boolean existsFile(UUID fileId);
}
