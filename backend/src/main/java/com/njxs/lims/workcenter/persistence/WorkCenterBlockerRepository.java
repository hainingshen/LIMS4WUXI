package com.njxs.lims.workcenter.persistence;

import com.njxs.lims.workcenter.domain.WorkCenterBlockerRecord;
import com.njxs.lims.workcenter.domain.WorkCenterScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkCenterBlockerRepository {

  WorkCenterBlockerRecord insert(WorkCenterBlockerRecord record, UUID actorUserId);

  List<WorkCenterBlockerRecord> list(WorkCenterScope scope, int limit, int offset);

  int count(WorkCenterScope scope);

  Optional<WorkCenterBlockerRecord> findById(UUID id);

  WorkCenterBlockerRecord update(WorkCenterBlockerRecord record, UUID actorUserId);

  boolean projectExists(UUID projectId);
}
