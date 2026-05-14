package com.njxs.lims.report.domain;

import java.util.UUID;

public record ReportRecord(
    UUID id,
    String bizNo,
    UUID projectId,
    UUID reportTemplateId,
    String templateCode,
    String templateVersion,
    String status,
    int currentVersionNo,
    boolean official,
    String watermarkType,
    String paymentGateStatus
) {
}
