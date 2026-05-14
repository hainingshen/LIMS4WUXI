package com.njxs.lims.study.api;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record StudyPlanCreateRequest(
    @NotNull UUID projectId,
    boolean requiresConditioning,
    Instant plannedStartAt,
    Instant plannedFinishAt
) {
}
