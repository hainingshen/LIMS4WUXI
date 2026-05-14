package com.njxs.lims.device.api;

import com.njxs.lims.device.domain.DeviceRecord;
import java.util.UUID;

public record DeviceResponse(
    UUID id,
    String deviceNo,
    String name,
    String model,
    String manufacturer,
    String location,
    UUID ownerUserId,
    boolean metrologyRequired,
    String status
) {

  public static DeviceResponse from(DeviceRecord record) {
    return new DeviceResponse(
        record.id(),
        record.deviceNo(),
        record.name(),
        record.model(),
        record.manufacturer(),
        record.location(),
        record.ownerUserId(),
        record.metrologyRequired(),
        record.status()
    );
  }
}
