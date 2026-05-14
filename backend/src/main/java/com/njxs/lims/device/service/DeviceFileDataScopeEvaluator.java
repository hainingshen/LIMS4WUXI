package com.njxs.lims.device.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.device.persistence.DeviceRepository;
import com.njxs.lims.permission.DataScopeEvaluator;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeviceFileDataScopeEvaluator implements DataScopeEvaluator {

  private final DeviceRepository deviceRepository;

  public DeviceFileDataScopeEvaluator(DeviceRepository deviceRepository) {
    this.deviceRepository = deviceRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return "DEVICE_FILE_READ".equals(actionCode) && "ASSIGNED_WORK_ORDER".equals(scope);
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    return firstUuid(args)
        .map(deviceId -> deviceRepository.deviceUsedByAssignedWorkOrder(deviceId, principal.userId()))
        .orElse(false);
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
