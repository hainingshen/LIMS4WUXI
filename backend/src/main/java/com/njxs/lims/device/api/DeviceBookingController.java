package com.njxs.lims.device.api;

import com.njxs.lims.audit.Audited;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.device.service.DeviceBookingService;
import com.njxs.lims.permission.RequiresAction;
import com.njxs.lims.permission.RequiresClient;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeviceBookingController {

  private final DeviceBookingService deviceBookingService;

  public DeviceBookingController(DeviceBookingService deviceBookingService) {
    this.deviceBookingService = deviceBookingService;
  }

  @GetMapping("/device-bookings")
  @RequiresClient(ClientType.INTERNAL)
  @RequiresAction("DEVICE_BOOKING_READ_ALL")
  public PageResponse<DeviceBookingResponse> list(
      @RequestParam(required = false) UUID deviceId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return deviceBookingService.list(deviceId, page, pageSize);
  }

  @PostMapping("/device-bookings")
  @ResponseStatus(HttpStatus.CREATED)
  @Audited(actionCode = "DEVICE_BOOK", entityType = "device_booking")
  @RequiresAction("DEVICE_BOOK")
  public DeviceBookingResponse create(@Valid @RequestBody DeviceBookingCreateRequest request) {
    return deviceBookingService.create(request, AuthContext.require());
  }

  @PostMapping("/device-bookings/{id}/override")
  @RequiresClient(ClientType.INTERNAL)
  @Audited(actionCode = "DEVICE_BOOKING_OVERRIDE", entityType = "device_booking")
  @RequiresAction("DEVICE_BOOKING_OVERRIDE")
  public DeviceBookingResponse override(
      @PathVariable UUID id,
      @Valid @RequestBody DeviceBookingOverrideRequest request
  ) {
    return deviceBookingService.override(id, request, AuthContext.require());
  }
}
