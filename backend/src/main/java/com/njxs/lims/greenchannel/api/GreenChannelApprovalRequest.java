package com.njxs.lims.greenchannel.api;

import com.njxs.lims.audit.AuditActionProvider;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

public record GreenChannelApprovalRequest(
    @NotBlank String stage,
    @NotBlank String outcome,
    String comment
) implements AuditActionProvider {

  @Override
  public String auditActionCode() {
    return switch (normalize(stage)) {
      case "DEPT" -> "GREEN_CHANNEL_DEPT_APPROVE";
      case "MANAGEMENT" -> "GREEN_CHANNEL_FINAL_APPROVE";
      default -> "GREEN_CHANNEL_APPROVE";
    };
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
