package com.njxs.lims.project.domain;

import com.njxs.lims.common.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public enum ProjectStatus {
  DRAFT,
  ACCEPTED,
  NONSTANDARD_REVIEW,
  QUOTABLE,
  QUOTED,
  CONTRACTED,
  WAITING_SAMPLE,
  SAMPLE_RECEIVED,
  PLAN_READY,
  IN_TESTING,
  REPORTING,
  PAYMENT_HOLD,
  RELEASED,
  ARCHIVED;

  public static ProjectStatus require(String status) {
    try {
      return ProjectStatus.valueOf(status);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new BusinessException(
          "ILLEGAL_PROJECT_STATUS",
          "项目状态不合法",
          HttpStatus.CONFLICT,
          Map.of("actual", status == null ? "" : status)
      );
    }
  }
}
