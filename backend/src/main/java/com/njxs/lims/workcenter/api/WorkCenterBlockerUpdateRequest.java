package com.njxs.lims.workcenter.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkCenterBlockerUpdateRequest(
    String blockerType,
    String severity,
    String description,
    String impactScope,
    Instant expectedRecoveryAt,
    UUID ownerUserId,
    List<UUID> collaboratorIds,
    String status
) {
}
