package com.njxs.lims.report.registry.api;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReportRegistryBatchDownloadRequest(
    @NotEmpty List<UUID> reportIds,
    Boolean includeDeliveryLog
) {

  public boolean includeDeliveryLogOrDefault() {
    return includeDeliveryLog == null || includeDeliveryLog;
  }
}
