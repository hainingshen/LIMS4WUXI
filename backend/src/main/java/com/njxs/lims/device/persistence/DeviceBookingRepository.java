package com.njxs.lims.device.persistence;

import com.njxs.lims.device.domain.DeviceBookingRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceBookingRepository {

  String nextBizNo(LocalDate date);

  List<DeviceBookingRecord> list(UUID deviceId, int limit, int offset);

  int count(UUID deviceId);

  Optional<DeviceBookingRecord> findById(UUID id);

  DeviceBookingRecord save(DeviceBookingRecord record, UUID actorUserId);

  Optional<DeviceBookingRecord> markNeedsReschedule(UUID id, String reason, UUID actorUserId);

  boolean existsActiveDevice(UUID deviceId);

  boolean existsWorkOrder(UUID workOrderId);

  boolean hasBookedOverlap(UUID deviceId, Instant startAt, Instant endAt, UUID excludedBookingId);
}
