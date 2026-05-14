package com.njxs.lims.report.registry.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportRegistryItemRecord(
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
    List<ReportRegistryVersionRecord> versions
) {

  public ReportRegistryItemRecord withVersions(List<ReportRegistryVersionRecord> versions) {
    return new ReportRegistryItemRecord(
        reportId,
        reportBizNo,
        projectId,
        projectBizNo,
        customerOrgId,
        templateCode,
        templateVersion,
        currentVersionNo,
        status,
        issuedAt,
        releasedAt,
        paymentGateStatus,
        deliveryCount,
        List.copyOf(versions)
    );
  }
}
