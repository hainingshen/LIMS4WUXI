package com.njxs.lims.workcenter.domain;

import java.time.Instant;

public record WorkCenterTaskRecord(
    String taskKey,
    String roleGroup,
    String taskType,
    String title,
    String bizNo,
    String customerOrgName,
    String status,
    int priority,
    Instant dueAt,
    String actionPath
) {
}
