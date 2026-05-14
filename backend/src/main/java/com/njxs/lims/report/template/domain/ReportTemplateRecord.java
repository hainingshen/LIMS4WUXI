package com.njxs.lims.report.template.domain;

import java.time.Instant;
import java.util.UUID;

public record ReportTemplateRecord(
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
}
