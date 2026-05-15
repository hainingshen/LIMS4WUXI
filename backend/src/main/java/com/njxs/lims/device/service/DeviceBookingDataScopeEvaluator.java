package com.njxs.lims.device.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.device.api.DeviceBookingCreateRequest;
import com.njxs.lims.permission.DataScopeEvaluator;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeviceBookingDataScopeEvaluator implements DataScopeEvaluator {

  @Override
  public boolean supports(String actionCode, String scope) {
    return "DEVICE_BOOK".equals(actionCode)
        && ("PORTAL_2_DAYS".equals(scope) || "INTERNAL_7_DAYS".equals(scope));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    Optional<Instant> startAt = Arrays.stream(args)
        .filter(DeviceBookingCreateRequest.class::isInstance)
        .map(DeviceBookingCreateRequest.class::cast)
        .map(DeviceBookingCreateRequest::startAt)
        .filter(Objects::nonNull)
        .findFirst();
    if (startAt.isEmpty()) {
      return false;
    }
    if ("PORTAL_2_DAYS".equals(scope)) {
      return principal.clientType() == ClientType.PORTAL
          && !startAt.get().isAfter(Instant.now().plus(Duration.ofDays(2)));
    }
    return principal.clientType() == ClientType.INTERNAL
        && !startAt.get().isAfter(Instant.now().plus(Duration.ofDays(7)));
  }
}
