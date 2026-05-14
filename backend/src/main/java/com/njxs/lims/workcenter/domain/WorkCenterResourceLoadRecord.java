package com.njxs.lims.workcenter.domain;

import java.util.UUID;

public record WorkCenterResourceLoadRecord(
    UUID userId,
    String displayName,
    int activeWorkOrders,
    int loadPercent
) {
}
