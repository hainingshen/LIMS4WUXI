package com.njxs.lims.device.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record DeviceFileUploadRequest(
    @NotBlank String fileType,
    @NotNull UUID fileId,
    String versionNo,
    LocalDate validFrom,
    LocalDate validUntil,
    Boolean isCurrent
) {

  public boolean currentOrDefault() {
    return isCurrent == null || isCurrent;
  }
}
