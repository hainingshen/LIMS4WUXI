package com.njxs.lims.study.persistence;

import com.njxs.lims.permission.InternalDataScope;
import com.njxs.lims.study.domain.WorkOrderRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository {

  Optional<WorkOrderRecord> findById(UUID id);

  List<WorkOrderRecord> list(int limit, int offset);

  int count();

  List<WorkOrderRecord> listVisibleTo(InternalDataScope scope, int limit, int offset);

  int countVisibleTo(InternalDataScope scope);

  WorkOrderRecord updateStatus(UUID id, String status, int progressPercent, UUID actorUserId);

  boolean existsAssigned(UUID id, UUID ownerUserId);

  boolean allCompletedByProject(UUID projectId);
}
