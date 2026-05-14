package com.njxs.lims.workcenter.api;

import com.njxs.lims.workcenter.domain.WorkCenterResourceLoadRecord;
import java.util.UUID;

public record WorkCenterResourceLoadResponse(
    UUID userId,
    String displayName,
    int activeWorkOrders,
    int loadPercent
) {

  public static WorkCenterResourceLoadResponse from(WorkCenterResourceLoadRecord record) {
    return new WorkCenterResourceLoadResponse(
        record.userId(),
        record.displayName(),
        record.activeWorkOrders(),
        record.loadPercent()
    );
  }
}
