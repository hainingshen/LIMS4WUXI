package com.njxs.lims.project.api;

import com.njxs.lims.audit.AuditActionProvider;
import jakarta.validation.constraints.NotBlank;

public record NonstandardReviewApprovalRequest(
    @NotBlank String stage,
    @NotBlank String outcome,
    String comment
) implements AuditActionProvider {

  @Override
  public String auditActionCode() {
    if ("BUSINESS".equals(stage)) {
      return "NONSTANDARD_BUSINESS_APPROVE";
    }
    return "NONSTANDARD_TECH_APPROVE";
  }
}
