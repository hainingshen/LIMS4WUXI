package com.njxs.lims.device.api;

import com.njxs.lims.device.domain.DeviceFileRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeviceFileResponse(
    UUID id,
    UUID deviceId,
    String fileType,
    UUID fileId,
    String versionNo,
    LocalDate validFrom,
    LocalDate validUntil,
    boolean isCurrent,
    UUID uploadedBy,
    Instant uploadedAt,
    String status
) {

  public static DeviceFileResponse from(DeviceFileRecord record) {
    return new DeviceFileResponse(
        record.id(),
        record.deviceId(),
        record.fileType(),
        record.fileId(),
        record.versionNo(),
        record.validFrom(),
        record.validUntil(),
        record.current(),
        record.uploadedBy(),
        record.uploadedAt(),
        record.status()
    );
  }
}
