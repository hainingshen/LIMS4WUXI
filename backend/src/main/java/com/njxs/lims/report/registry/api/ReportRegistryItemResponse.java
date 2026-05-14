package com.njxs.lims.report.registry.api;

import com.njxs.lims.report.registry.domain.ReportRegistryItemRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportRegistryItemResponse(
    UUID reportId,
    String reportBizNo,
    UUID projectId,
    String projectBizNo,
    UUID customerOrgId,
    String templateCode,
    String templateVersion,
    int currentVersionNo,
    String status,
    Instant issuedAt,
    Instant releasedAt,
    String paymentGateStatus,
    int deliveryCount,
    List<ReportRegistryVersionResponse> versions
) {

  public static ReportRegistryItemResponse from(ReportRegistryItemRecord record) {
    return new ReportRegistryItemResponse(
        record.reportId(),
        record.reportBizNo(),
        record.projectId(),
        record.projectBizNo(),
        record.customerOrgId(),
        record.templateCode(),
        record.templateVersion(),
        record.currentVersionNo(),
        record.status(),
        record.issuedAt(),
        record.releasedAt(),
        record.paymentGateStatus(),
        record.deliveryCount(),
        record.versions().stream()
            .map(ReportRegistryVersionResponse::from)
            .toList()
    );
  }
}
