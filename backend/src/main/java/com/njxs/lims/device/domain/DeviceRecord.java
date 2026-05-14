package com.njxs.lims.device.domain;

import java.util.UUID;

public record DeviceRecord(
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
}
