package com.njxs.lims.workcenter.domain;

import java.util.UUID;

public record WorkCenterProjectRecord(
    UUID projectId,
    String projectBizNo,
    String customerOrgName,
    String status,
    String healthStatus,
    int healthScore,
    String mainBlocker
) {
}
