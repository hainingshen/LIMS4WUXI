package com.njxs.lims.report.registry.api;

import com.njxs.lims.report.registry.domain.ReportRegistryVersionRecord;
import java.time.Instant;
import java.util.UUID;

public record ReportRegistryVersionResponse(
    UUID id,
    UUID reportId,
    int versionNo,
    String status,
    UUID docxFileId,
    UUID pdfFileId,
    Instant issuedAt
) {

  public static ReportRegistryVersionResponse from(ReportRegistryVersionRecord record) {
    return new ReportRegistryVersionResponse(
        record.id(),
        record.reportId(),
        record.versionNo(),
        record.status(),
        record.docxFileId(),
        record.pdfFileId(),
        record.issuedAt()
    );
  }
}
