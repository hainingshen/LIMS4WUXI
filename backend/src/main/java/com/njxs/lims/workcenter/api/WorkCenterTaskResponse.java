package com.njxs.lims.workcenter.api;

import com.njxs.lims.workcenter.domain.WorkCenterTaskRecord;
import java.time.Instant;

public record WorkCenterTaskResponse(
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

  public static WorkCenterTaskResponse from(WorkCenterTaskRecord record) {
    return new WorkCenterTaskResponse(
        record.taskKey(),
        record.roleGroup(),
        record.taskType(),
        record.title(),
        record.bizNo(),
        record.customerOrgName(),
        record.status(),
        record.priority(),
        record.dueAt(),
        record.actionPath()
    );
  }
}
