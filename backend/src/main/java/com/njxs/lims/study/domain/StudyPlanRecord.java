package com.njxs.lims.study.domain;

import java.time.Instant;
import java.util.UUID;

public record StudyPlanRecord(
    UUID id,
    String bizNo,
    UUID projectId,
    String status,
    boolean requiresConditioning,
    boolean requiresGlp,
    boolean requiresAnimalTest,
    Instant plannedStartAt,
    Instant plannedFinishAt,
    Instant sdApprovedAt,
    Instant qaApprovedAt,
    Instant ethicsApprovedAt
) {
}
