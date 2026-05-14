package com.njxs.lims.workcenter.api;

import com.njxs.lims.workcenter.domain.WorkCenterTrendRecord;
import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkCenterTrendResponse(
    String metricCode,
    LocalDate metricDate,
    BigDecimal value
) {

  public static WorkCenterTrendResponse from(WorkCenterTrendRecord record) {
    return new WorkCenterTrendResponse(record.metricCode(), record.metricDate(), record.value());
  }
}
