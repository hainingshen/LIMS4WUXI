package com.njxs.lims.study.domain;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderRecord(
    UUID id,
    String bizNo,
    UUID studyPlanId,
    UUID projectId,
    UUID sampleId,
    UUID ownerUserId,
    String status,
    int progressPercent,
    Instant plannedFinishAt
) {
}
