package com.njxs.lims.device.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.device.api.DeviceBookingCreateRequest;
import com.njxs.lims.device.api.DeviceBookingOverrideRequest;
import com.njxs.lims.device.api.DeviceBookingResponse;
import com.njxs.lims.device.domain.DeviceBookingRecord;
import com.njxs.lims.device.persistence.DeviceBookingRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeviceBookingService {

  private final DeviceBookingRepository deviceBookingRepository;

  public DeviceBookingService(DeviceBookingRepository deviceBookingRepository) {
    this.deviceBookingRepository = deviceBookingRepository;
  }

  public PageResponse<DeviceBookingResponse> list(UUID deviceId, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        deviceBookingRepository.list(deviceId, normalizedPageSize, offset).stream()
            .map(DeviceBookingResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        deviceBookingRepository.count(deviceId)
    );
  }

  @Transactional
  public DeviceBookingResponse create(DeviceBookingCreateRequest request, AuthPrincipal principal) {
    requireActiveDevice(request.deviceId());
    validateRange(request.startAt(), request.endAt());
    validateAdvanceWindow(request.startAt(), principal);
    String purpose = requireText("purpose", request.purpose());
    if (request.workOrderId() != null && !deviceBookingRepository.existsWorkOrder(request.workOrderId())) {
      throw notFound("work_order", request.workOrderId());
    }
    if (principal.clientType() == ClientType.INTERNAL && request.workOrderId() == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("workOrderId", "required for internal inspector booking")
      );
    }
    if (deviceBookingRepository.hasBookedOverlap(request.deviceId(), request.startAt(), request.endAt(), null)) {
      throw new BusinessException(
          "DEVICE_BOOKING_CONFLICT",
          "设备预约时间冲突",
          HttpStatus.CONFLICT,
          Map.of("deviceId", request.deviceId(), "startAt", request.startAt(), "endAt", request.endAt())
      );
    }
    LocalDate bizDate = request.startAt().atZone(ZoneOffset.UTC).toLocalDate();
    DeviceBookingRecord record = new DeviceBookingRecord(
        UUID.randomUUID(),
        deviceBookingRepository.nextBizNo(bizDate),
        request.deviceId(),
        principal.userId(),
        principal.clientType() == ClientType.PORTAL ? "EXTERNAL_USER" : "INSPECTOR",
        principal.clientType() == ClientType.PORTAL ? null : request.workOrderId(),
        purpose,
        request.startAt(),
        request.endAt(),
        "BOOKED",
        null
    );
    return DeviceBookingResponse.from(deviceBookingRepository.save(record, principal.userId()));
  }

  @Transactional
  public DeviceBookingResponse override(UUID id, DeviceBookingOverrideRequest request, AuthPrincipal principal) {
    DeviceBookingRecord booking = deviceBookingRepository.findById(id)
        .orElseThrow(() -> notFound("device_booking", id));
    if (!"BOOKED".equals(booking.status()) || !"EXTERNAL_USER".equals(booking.bookerType())) {
      throw new BusinessException(
          "DEVICE_BOOKING_OVERRIDE_INVALID",
          "只能覆盖外部已预约记录",
          HttpStatus.CONFLICT,
          Map.of("bookingId", id, "status", booking.status(), "bookerType", booking.bookerType())
      );
    }
    validateRange(request.replacementStartAt(), request.replacementEndAt());
    if (deviceBookingRepository.hasBookedOverlap(
        booking.deviceId(),
        request.replacementStartAt(),
        request.replacementEndAt(),
        booking.id()
    )) {
      throw new BusinessException(
          "DEVICE_BOOKING_CONFLICT",
          "设备预约时间冲突",
          HttpStatus.CONFLICT,
          Map.of("deviceId", booking.deviceId())
      );
    }
    String reason = requireText("reason", request.reason());
    return deviceBookingRepository.markNeedsReschedule(id, reason, principal.userId())
        .map(DeviceBookingResponse::from)
        .orElseThrow(() -> notFound("device_booking", id));
  }

  private void requireActiveDevice(UUID deviceId) {
    if (!deviceBookingRepository.existsActiveDevice(deviceId)) {
      throw notFound("device", deviceId);
    }
  }

  private void validateRange(Instant startAt, Instant endAt) {
    if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("timeRange", "startAt must be before endAt")
      );
    }
    if (startAt.isBefore(Instant.now())) {
      throw new BusinessException(
          "DEVICE_BOOKING_TIME_INVALID",
          "设备预约时间不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("startAt", startAt)
      );
    }
    if (!alignedToHalfHour(startAt) || !alignedToHalfHour(endAt)) {
      throw new BusinessException(
          "DEVICE_BOOKING_GRANULARITY_INVALID",
          "设备预约必须按半小时粒度",
          HttpStatus.BAD_REQUEST,
          Map.of("startAt", startAt, "endAt", endAt)
      );
    }
  }

  private void validateAdvanceWindow(Instant startAt, AuthPrincipal principal) {
    Duration window = principal.clientType() == ClientType.PORTAL ? Duration.ofDays(2) : Duration.ofDays(7);
    if (startAt.isAfter(Instant.now().plus(window))) {
      throw new BusinessException(
          "DEVICE_BOOKING_ADVANCE_WINDOW_EXCEEDED",
          "设备预约超过允许提前天数",
          HttpStatus.FORBIDDEN,
          Map.of("startAt", startAt, "clientType", principal.clientType())
      );
    }
  }

  private boolean alignedToHalfHour(Instant instant) {
    return instant.atZone(ZoneOffset.UTC).getMinute() % 30 == 0
        && instant.atZone(ZoneOffset.UTC).getSecond() == 0
        && instant.atZone(ZoneOffset.UTC).getNano() == 0;
  }

  private String requireText(String field, String value) {
    String normalized = value == null || value.isBlank() ? null : value.trim();
    if (normalized == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "required")
      );
    }
    return normalized;
  }

  private BusinessException notFound(String entityType, UUID entityId) {
    return new BusinessException(
        "NOT_FOUND",
        "资源不存在",
        HttpStatus.NOT_FOUND,
        Map.of("entityType", entityType, "entityId", entityId)
    );
  }
}
