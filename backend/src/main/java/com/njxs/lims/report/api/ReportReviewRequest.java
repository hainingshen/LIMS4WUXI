package com.njxs.lims.report.api;

import com.njxs.lims.audit.AuditActionProvider;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

public record ReportReviewRequest(
    @NotBlank String stage,
    @NotBlank String outcome,
    String comment
) implements AuditActionProvider {

  @Override
  public String auditActionCode() {
    return switch (normalize(stage)) {
      case "QC" -> "REPORT_QC_REVIEW";
      case "QA" -> "REPORT_QA_REVIEW";
      case "SD" -> "REPORT_SD_REVIEW";
      default -> "REPORT_REVIEW";
    };
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
