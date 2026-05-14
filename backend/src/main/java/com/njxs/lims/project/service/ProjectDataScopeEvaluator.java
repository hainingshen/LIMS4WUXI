package com.njxs.lims.project.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.project.persistence.ProjectRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProjectDataScopeEvaluator implements DataScopeEvaluator {

  private static final String ACTION_NONSTANDARD_REVIEW_START = "NONSTANDARD_REVIEW_START";
  private static final String ACTION_NONSTANDARD_TECH_APPROVE = "NONSTANDARD_TECH_APPROVE";
  private static final String ACTION_NONSTANDARD_BUSINESS_APPROVE = "NONSTANDARD_BUSINESS_APPROVE";
  private static final String ACTION_QUOTATION_CREATE = "QUOTATION_CREATE";

  private final ProjectRepository projectRepository;

  public ProjectDataScopeEvaluator(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return (ACTION_NONSTANDARD_REVIEW_START.equals(actionCode) && "ASSIGNED_PROJECT".equals(scope))
        || (ACTION_NONSTANDARD_TECH_APPROVE.equals(actionCode) && "TASK".equals(scope))
        || (ACTION_NONSTANDARD_BUSINESS_APPROVE.equals(actionCode) && "TASK".equals(scope))
        || (ACTION_QUOTATION_CREATE.equals(actionCode) && "AFTER_REVIEW".equals(scope));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    Optional<UUID> projectId = firstUuid(args);
    if (projectId.isEmpty()) {
      return false;
    }
    if (ACTION_NONSTANDARD_REVIEW_START.equals(actionCode) || ACTION_QUOTATION_CREATE.equals(actionCode)) {
      return projectRepository.existsByAssignedSales(projectId.get(), principal.userId());
    }
    if (ACTION_NONSTANDARD_TECH_APPROVE.equals(actionCode)) {
      return projectRepository.existsByTechnicalOwner(projectId.get(), principal.userId());
    }
    return projectRepository.existsByBusinessOwner(projectId.get(), principal.userId());
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
