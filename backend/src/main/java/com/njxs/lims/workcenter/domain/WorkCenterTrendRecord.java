package com.njxs.lims.workcenter.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkCenterTrendRecord(
    String metricCode,
    LocalDate metricDate,
    BigDecimal value
) {
}
