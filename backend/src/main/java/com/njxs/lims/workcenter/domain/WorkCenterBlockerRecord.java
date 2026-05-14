package com.njxs.lims.workcenter.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkCenterBlockerRecord(
    UUID id,
    UUID projectId,
    String blockerType,
    String severity,
    String description,
    String impactScope,
    Instant expectedRecoveryAt,
    UUID ownerUserId,
    List<UUID> collaboratorIds,
    String status,
    String resolution
) {
  public WorkCenterBlockerRecord {
    collaboratorIds = collaboratorIds == null ? List.of() : List.copyOf(collaboratorIds);
  }
}
