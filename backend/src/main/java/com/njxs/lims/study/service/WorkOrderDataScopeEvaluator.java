package com.njxs.lims.study.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.study.persistence.WorkOrderRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkOrderDataScopeEvaluator implements DataScopeEvaluator {

  private final WorkOrderRepository workOrderRepository;

  public WorkOrderDataScopeEvaluator(WorkOrderRepository workOrderRepository) {
    this.workOrderRepository = workOrderRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return ("WORK_ORDER_EXECUTE".equals(actionCode) || "RAW_RECORD_SUBMIT".equals(actionCode))
        && "ASSIGNED_WORK_ORDER".equals(scope);
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    return firstUuid(args)
        .map(id -> workOrderRepository.existsAssigned(id, principal.userId()))
        .orElse(false);
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
