package com.njxs.lims.study.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.permission.DataScopeEvaluator;
import com.njxs.lims.project.persistence.ProjectRepository;
import com.njxs.lims.study.api.StudyPlanCreateRequest;
import com.njxs.lims.study.persistence.StudyPlanRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StudyPlanDataScopeEvaluator implements DataScopeEvaluator {

  private final ProjectRepository projectRepository;
  private final StudyPlanRepository studyPlanRepository;

  public StudyPlanDataScopeEvaluator(
      ProjectRepository projectRepository,
      StudyPlanRepository studyPlanRepository
  ) {
    this.projectRepository = projectRepository;
    this.studyPlanRepository = studyPlanRepository;
  }

  @Override
  public boolean supports(String actionCode, String scope) {
    return ("PLAN_CREATE".equals(actionCode) && "TECH_SCOPE".equals(scope))
        || ("PLAN_REVIEW_SD".equals(actionCode) && "TASK".equals(scope))
        || ("PLAN_DISPATCH".equals(actionCode) && "READY_TO_DISPATCH".equals(scope));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    if ("PLAN_CREATE".equals(actionCode)) {
      return createRequest(args)
          .map(StudyPlanCreateRequest::projectId)
          .map(projectId -> projectRepository.existsByTechnicalOwner(projectId, principal.userId()))
          .orElse(false);
    }
    Optional<UUID> planId = firstUuid(args);
    if (planId.isEmpty()) {
      return false;
    }
    if ("PLAN_REVIEW_SD".equals(actionCode)) {
      return studyPlanRepository.findById(planId.get()).isPresent();
    }
    return studyPlanRepository.findById(planId.get())
        .map(plan -> "READY_TO_DISPATCH".equals(plan.status()) || "PENDING_DISPATCH".equals(plan.status()))
        .orElse(false);
  }

  private Optional<StudyPlanCreateRequest> createRequest(Object[] args) {
    return Arrays.stream(args)
        .filter(StudyPlanCreateRequest.class::isInstance)
        .map(StudyPlanCreateRequest.class::cast)
        .findFirst();
  }

  private Optional<UUID> firstUuid(Object[] args) {
    return Arrays.stream(args)
        .filter(UUID.class::isInstance)
        .map(UUID.class::cast)
        .findFirst();
  }
}
