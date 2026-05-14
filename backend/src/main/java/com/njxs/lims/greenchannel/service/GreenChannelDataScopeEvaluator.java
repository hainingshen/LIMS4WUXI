package com.njxs.lims.greenchannel.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.greenchannel.api.GreenChannelCreateRequest;
import com.njxs.lims.greenchannel.persistence.GreenChannelRepository;
import com.njxs.lims.permission.DataScopeEvaluator;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GreenChannelDataScopeEvaluator implements DataScopeEvaluator {

  private static final Set<String> TASK_ACTIONS = Set.of(
      "GREEN_CHANNEL_DEPT_APPROVE",
      "GREEN_CHANNEL_FINAL_APPROVE"
  );

  private final GreenChannelRepository greenChannelRepository;

  public GreenChannelDataScopeEvaluator(GreenChannelRepository greenChannelRepository) {
    this.greenChannelRepository = greenChannelRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return ("GREEN_CHANNEL_CREATE".equals(actionCode) && "ASSIGNED_PROJECT".equals(scope))
        || (TASK_ACTIONS.contains(actionCode) && "TASK".equals(scope));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    if (TASK_ACTIONS.contains(actionCode)) {
      return firstUuid(args).map(greenChannelRepository::existsById).orElse(false);
    }
    return request(args)
        .filter(request -> request.projectId() != null && request.reportId() != null)
        .filter(request -> greenChannelRepository.reportBelongsToProject(request.reportId(), request.projectId()))
        .map(request -> greenChannelRepository.reportAssignedSales(request.reportId(), principal.userId()))
        .orElse(false);
  }

  private Optional<GreenChannelCreateRequest> request(Object[] args) {
    return Arrays.stream(args)
        .filter(GreenChannelCreateRequest.class::isInstance)
        .map(GreenChannelCreateRequest.class::cast)
        .findFirst();
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
