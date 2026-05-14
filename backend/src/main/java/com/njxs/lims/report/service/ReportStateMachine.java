package com.njxs.lims.report.service;

import com.njxs.lims.approval.domain.ApprovalOutcome;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.report.domain.ReportRecord;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReportStateMachine {

  public String afterSubmitQc(ReportRecord report) {
    requireStatus(report, Set.of("DRAFT", "REVISION_DRAFT"));
    return "QC_REVIEWING";
  }

  public String afterReview(ReportRecord report, String stage, ApprovalOutcome outcome) {
    return switch (stage) {
      case "QC" -> afterStage(report, outcome, "QC_REVIEWING", "QA_REVIEWING");
      case "QA" -> afterStage(report, outcome, "QA_REVIEWING", "SD_REVIEWING");
      case "SD" -> afterStage(report, outcome, "SD_REVIEWING", "PENDING_ISSUE");
      default -> throw new BusinessException(
          "REPORT_REVIEW_STAGE_INVALID",
          "报告审核阶段不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("stage", stage)
      );
    };
  }

  public String afterIssue(ReportRecord report) {
    requireStatus(report, Set.of("PENDING_ISSUE"));
    return "ISSUED_PAYMENT_LOCKED";
  }

  private String afterStage(
      ReportRecord report,
      ApprovalOutcome outcome,
      String expectedStatus,
      String approvedStatus
  ) {
    requireStatus(report, Set.of(expectedStatus));
    return switch (outcome) {
      case APPROVED -> approvedStatus;
      case REJECTED -> "DRAFT";
      default -> throw new BusinessException(
          "REPORT_REVIEW_OUTCOME_INVALID",
          "报告审核结论不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("outcome", outcome)
      );
    };
  }

  private void requireStatus(ReportRecord report, Set<String> expected) {
    if (!expected.contains(report.status())) {
      throw new BusinessException(
          "REPORT_STATUS_CONFLICT",
          "报告当前状态不可执行该操作",
          HttpStatus.CONFLICT,
          Map.of("expected", expected, "actual", report.status())
      );
    }
  }
}
