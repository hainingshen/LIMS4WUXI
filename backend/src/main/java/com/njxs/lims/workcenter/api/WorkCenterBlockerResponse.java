package com.njxs.lims.workcenter.api;

import com.njxs.lims.workcenter.domain.WorkCenterBlockerRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkCenterBlockerResponse(
    UUID id,
    UUID projectId,
    String blockerType,
    String severity,
    String description,
    String impactScope,
    UUID ownerUserId,
    List<UUID> collaboratorIds,
    Instant expectedRecoveryAt,
    String status,
    String resolution
) {

  public static WorkCenterBlockerResponse from(WorkCenterBlockerRecord record) {
    return new WorkCenterBlockerResponse(
        record.id(),
        record.projectId(),
        record.blockerType(),
        record.severity(),
        record.description(),
        record.impactScope(),
        record.ownerUserId(),
        record.collaboratorIds(),
        record.expectedRecoveryAt(),
        record.status(),
        record.resolution()
    );
  }
}
