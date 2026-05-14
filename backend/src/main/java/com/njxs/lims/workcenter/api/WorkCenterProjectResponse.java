package com.njxs.lims.workcenter.api;

import com.njxs.lims.workcenter.domain.WorkCenterProjectRecord;
import java.util.UUID;

public record WorkCenterProjectResponse(
    UUID projectId,
    String projectBizNo,
    String customerOrgName,
    String status,
    String healthStatus,
    int healthScore,
    String mainBlocker
) {

  public static WorkCenterProjectResponse from(WorkCenterProjectRecord record) {
    return new WorkCenterProjectResponse(
        record.projectId(),
        record.projectBizNo(),
        record.customerOrgName(),
        record.status(),
        record.healthStatus(),
        record.healthScore(),
        record.mainBlocker()
    );
  }
}
