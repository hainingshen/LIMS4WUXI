package com.njxs.lims.report.template.api;

import com.njxs.lims.report.template.domain.ReportTemplateRecord;
import java.time.Instant;
import java.util.UUID;

public record ReportTemplateResponse(
    UUID id,
    String templateCode,
    String templateVersion,
    String templateName,
    String templateType,
    String automationLevel,
    UUID fileId,
    String status,
    Instant publishedAt,
    UUID publishedBy,
    String legacyTemplateName
) {

  public static ReportTemplateResponse from(ReportTemplateRecord record) {
    return new ReportTemplateResponse(
        record.id(),
        record.templateCode(),
        record.templateVersion(),
        record.templateName(),
        record.templateType(),
        record.automationLevel(),
        record.fileId(),
        record.status(),
        record.publishedAt(),
        record.publishedBy(),
        record.legacyTemplateName()
    );
  }
}
