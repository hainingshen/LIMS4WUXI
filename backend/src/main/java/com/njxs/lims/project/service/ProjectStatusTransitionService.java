package com.njxs.lims.project.service;

import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.project.domain.Project;
import com.njxs.lims.project.domain.ProjectStatus;
import com.njxs.lims.project.domain.ProjectStatusChangedEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProjectStatusTransitionService {

  private static final Map<ProjectStatus, Set<ProjectStatus>> ALLOWED_TARGETS = allowedTargets();

  private final ApplicationEventPublisher eventPublisher;

  public ProjectStatusTransitionService(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public void transition(Project project, ProjectStatus target, String reason) {
    ProjectStatus source = ProjectStatus.require(project.getStatus());
    if (target == null) {
      throw new BusinessException(
          "ILLEGAL_PROJECT_STATUS_TRANSITION",
          "项目状态迁移不合法",
          HttpStatus.CONFLICT,
          Map.of("source", source.name(), "target", "")
      );
    }
    if (!allowedTargets(source).contains(target)) {
      throw new BusinessException(
          "ILLEGAL_PROJECT_STATUS_TRANSITION",
          "项目状态迁移不合法",
          HttpStatus.CONFLICT,
          Map.of("source", source.name(), "target", target.name())
      );
    }
    project.setStatus(target.name());
    eventPublisher.publishEvent(new ProjectStatusChangedEvent(project.getId(), source.name(), target.name(), reason));
  }

  public Set<ProjectStatus> allowedTargets(ProjectStatus status) {
    return ALLOWED_TARGETS.getOrDefault(status, Set.of());
  }

  private static Map<ProjectStatus, Set<ProjectStatus>> allowedTargets() {
    EnumMap<ProjectStatus, Set<ProjectStatus>> targets = new EnumMap<>(ProjectStatus.class);
    targets.put(ProjectStatus.DRAFT, Set.of(ProjectStatus.ACCEPTED));
    targets.put(ProjectStatus.ACCEPTED, Set.of(ProjectStatus.NONSTANDARD_REVIEW, ProjectStatus.QUOTABLE));
    targets.put(ProjectStatus.NONSTANDARD_REVIEW, Set.of(ProjectStatus.ACCEPTED, ProjectStatus.QUOTABLE));
    targets.put(ProjectStatus.QUOTABLE, Set.of(ProjectStatus.QUOTED));
    targets.put(ProjectStatus.QUOTED, Set.of(ProjectStatus.CONTRACTED));
    targets.put(ProjectStatus.CONTRACTED, Set.of(ProjectStatus.WAITING_SAMPLE, ProjectStatus.SAMPLE_RECEIVED));
    targets.put(ProjectStatus.WAITING_SAMPLE, Set.of(ProjectStatus.SAMPLE_RECEIVED));
    targets.put(ProjectStatus.SAMPLE_RECEIVED, Set.of(ProjectStatus.PLAN_READY));
    targets.put(ProjectStatus.PLAN_READY, Set.of(ProjectStatus.IN_TESTING));
    targets.put(ProjectStatus.IN_TESTING, Set.of(ProjectStatus.REPORTING));
    targets.put(ProjectStatus.REPORTING, Set.of(ProjectStatus.PAYMENT_HOLD));
    targets.put(ProjectStatus.PAYMENT_HOLD, Set.of(ProjectStatus.RELEASED));
    targets.put(ProjectStatus.RELEASED, Set.of(ProjectStatus.ARCHIVED));
    targets.put(ProjectStatus.ARCHIVED, Set.of());
    return Map.copyOf(targets);
  }
}
