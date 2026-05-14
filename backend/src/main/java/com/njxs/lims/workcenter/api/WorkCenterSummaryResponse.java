package com.njxs.lims.workcenter.api;

import com.njxs.lims.workcenter.domain.WorkCenterSummaryRecord;
import java.time.Instant;

public record WorkCenterSummaryResponse(
    int normalProjects,
    int warningProjects,
    int riskProjects,
    int myTodos,
    int overdueTasks,
    Instant generatedAt
) {

  public static WorkCenterSummaryResponse from(WorkCenterSummaryRecord record) {
    return new WorkCenterSummaryResponse(
        record.normalProjects(),
        record.warningProjects(),
        record.riskProjects(),
        record.myTodos(),
        record.overdueTasks(),
        record.generatedAt()
    );
  }
}
