package com.njxs.lims.study.api;

import com.njxs.lims.study.domain.StudyPlanRecord;
import java.time.Instant;
import java.util.UUID;

public record StudyPlanResponse(
    UUID id,
    String bizNo,
    UUID projectId,
    String status,
    boolean requiresConditioning,
    boolean requiresGlp,
    boolean requiresAnimalTest,
    Instant plannedStartAt,
    Instant plannedFinishAt
) {

  public static StudyPlanResponse from(StudyPlanRecord record) {
    return new StudyPlanResponse(
        record.id(),
        record.bizNo(),
        record.projectId(),
        record.status(),
        record.requiresConditioning(),
        record.requiresGlp(),
        record.requiresAnimalTest(),
        record.plannedStartAt(),
        record.plannedFinishAt()
    );
  }
}
