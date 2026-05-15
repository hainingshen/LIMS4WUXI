package com.njxs.lims.device.api;

import com.njxs.lims.device.domain.DeviceBookingRecord;
import java.time.Instant;
import java.util.UUID;

public record DeviceBookingResponse(
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
) {

  public static DeviceBookingResponse from(DeviceBookingRecord record) {
    return new DeviceBookingResponse(
        record.id(),
        record.bizNo(),
        record.deviceId(),
        record.bookerId(),
        record.bookerType(),
        record.workOrderId(),
        record.purpose(),
        record.startAt(),
        record.endAt(),
        record.status(),
        record.overrideReason()
    );
  }
}
