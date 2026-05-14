package com.njxs.lims.report.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReportGenerateDraftRequest(
    @NotNull UUID projectId,
    @NotBlank String templateCode,
    @NotEmpty List<UUID> workOrderIds
) {
}
