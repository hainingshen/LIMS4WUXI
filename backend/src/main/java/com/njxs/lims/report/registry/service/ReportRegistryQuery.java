package com.njxs.lims.report.registry.service;

import java.time.Instant;
import java.util.UUID;

public record ReportRegistryQuery(
    String keyword,
    UUID customerOrgId,
    UUID projectId,
    String reportBizNo,
    String templateType,
    String status,
    Instant issuedFrom,
    Instant issuedTo
) {
}
