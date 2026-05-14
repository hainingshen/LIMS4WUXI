package com.njxs.lims.study.persistence;

import com.njxs.lims.study.domain.WorkOrderRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository {

  Optional<WorkOrderRecord> findById(UUID id);

  List<WorkOrderRecord> list(int limit, int offset);

  int count();

  WorkOrderRecord updateStatus(UUID id, String status, int progressPercent, UUID actorUserId);

  boolean existsAssigned(UUID id, UUID ownerUserId);
}
