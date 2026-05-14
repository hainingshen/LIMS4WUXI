package com.njxs.lims.workcenter.domain;

import java.time.Instant;
import java.util.UUID;

public record ProjectHealthContext(
    UUID projectId,
    Instant lastProgressAt,
    int overdueTaskCount,
    boolean forcedRisk
) {
}
