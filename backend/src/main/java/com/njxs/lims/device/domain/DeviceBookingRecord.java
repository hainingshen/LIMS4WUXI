package com.njxs.lims.device.domain;

import java.time.Instant;
import java.util.UUID;

public record DeviceBookingRecord(
    UUID id,
    String bizNo,
    UUID deviceId,
    UUID bookerId,
    String bookerType,
    UUID workOrderId,
    String purpose,
    Instant startAt,
    Instant endAt,
    String status,
    String overrideReason
) {}
