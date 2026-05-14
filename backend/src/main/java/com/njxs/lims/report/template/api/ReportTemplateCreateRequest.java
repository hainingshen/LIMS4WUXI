package com.njxs.lims.report.template.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReportTemplateCreateRequest(
    @NotBlank String templateCode,
    @NotBlank String templateVersion,
    @NotBlank String templateName,
    @NotBlank String templateType,
    @NotBlank String automationLevel,
    @NotNull UUID fileId,
    String legacyTemplateName
) {
}
