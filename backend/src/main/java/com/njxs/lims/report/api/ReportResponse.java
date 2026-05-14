package com.njxs.lims.report.api;

import com.njxs.lims.report.domain.ReportRecord;
import java.util.UUID;

public record ReportResponse(
    UUID id,
    String bizNo,
    UUID projectId,
    UUID reportTemplateId,
    String templateCode,
    String templateVersion,
    String status,
    String watermarkType,
    String paymentGateStatus
) {

  public static ReportResponse from(ReportRecord record) {
    return new ReportResponse(
        record.id(),
        record.bizNo(),
        record.projectId(),
        record.reportTemplateId(),
        record.templateCode(),
        record.templateVersion(),
        record.status(),
        record.watermarkType(),
        record.paymentGateStatus()
    );
  }
}
