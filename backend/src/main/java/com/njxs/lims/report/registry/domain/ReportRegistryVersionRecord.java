package com.njxs.lims.report.registry.domain;

import java.time.Instant;
import java.util.UUID;

public record ReportRegistryVersionRecord(
    UUID id,
    UUID reportId,
    int versionNo,
    String status,
    UUID docxFileId,
    UUID pdfFileId,
    Instant issuedAt
) {
}
