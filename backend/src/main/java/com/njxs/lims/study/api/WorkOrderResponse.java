package com.njxs.lims.study.api;

import com.njxs.lims.study.domain.WorkOrderRecord;
import java.time.Instant;
import java.util.UUID;

public record WorkOrderResponse(
    UUID id,
    String bizNo,
    UUID projectId,
    UUID sampleId,
    UUID ownerUserId,
    String status,
    int progressPercent,
    Instant plannedFinishAt
) {

  public static WorkOrderResponse from(WorkOrderRecord record) {
    return new WorkOrderResponse(
        record.id(),
        record.bizNo(),
        record.projectId(),
        record.sampleId(),
        record.ownerUserId(),
        record.status(),
        record.progressPercent(),
        record.plannedFinishAt()
    );
  }
}
