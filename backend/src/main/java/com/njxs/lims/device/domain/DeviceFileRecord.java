package com.njxs.lims.device.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeviceFileRecord(
    UUID id,
    UUID deviceId,
    String fileType,
    UUID fileId,
    String versionNo,
    LocalDate validFrom,
    LocalDate validUntil,
    boolean current,
    UUID uploadedBy,
    Instant uploadedAt,
    String status
) {
}
