package com.njxs.lims.workcenter.domain;

import java.time.Instant;

public record WorkCenterSummaryRecord(
    int normalProjects,
    int warningProjects,
    int riskProjects,
    int myTodos,
    int overdueTasks,
    Instant generatedAt
) {
}
