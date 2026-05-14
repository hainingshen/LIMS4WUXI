package com.njxs.lims.approval.domain;

import com.njxs.lims.common.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public enum ApprovalOutcome {
  APPROVED,
  REJECTED,
  NA;

  public static ApprovalOutcome require(String outcome) {
    try {
      return ApprovalOutcome.valueOf(outcome);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new BusinessException(
          "INVALID_APPROVAL_OUTCOME",
          "审批结论不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("actual", outcome == null ? "" : outcome)
      );
    }
  }
}
