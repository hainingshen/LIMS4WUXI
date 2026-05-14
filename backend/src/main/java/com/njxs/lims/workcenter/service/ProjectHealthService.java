package com.njxs.lims.workcenter.service;

import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.workcenter.domain.ProjectHealth;
import com.njxs.lims.workcenter.domain.ProjectHealthContext;
import com.njxs.lims.workcenter.persistence.ProjectHealthRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProjectHealthService {

  private static final String NORMAL = "NORMAL";
  private static final String WARNING = "WARNING";
  private static final String RISK = "RISK";

  private static final String STALE_DAYS = "WORK_CENTER_STALE_DAYS";
  private static final String STALE_PENALTY = "WORK_CENTER_STALE_PENALTY";
  private static final String NORMAL_SCORE = "WORK_CENTER_NORMAL_SCORE";
  private static final String RISK_SCORE = "WORK_CENTER_RISK_SCORE";
  private static final String OVERDUE_TASK_PENALTY = "WORK_CENTER_OVERDUE_TASK_PENALTY";
  private static final String OVERDUE_TASK_MAX_PENALTY = "WORK_CENTER_OVERDUE_TASK_MAX_PENALTY";

  private final ProjectHealthRepository repository;

  public ProjectHealthService(ProjectHealthRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public ProjectHealth recalculate(UUID projectId) {
    ProjectHealthContext context = repository.context(projectId)
        .orElseThrow(() -> notFound("project", projectId));
    ProjectHealth health = evaluate(context);
    repository.updateHealth(projectId, health);
    return health;
  }

  public ProjectHealth evaluate(ProjectHealthContext context) {
    int score = 100;
    score -= stalePenalty(context);
    score -= overdueTaskPenalty(context);
    score = clamp(score);

    String status = context.forcedRisk() ? RISK : statusByScore(score);
    return ProjectHealth.of(score, status);
  }

  private int stalePenalty(ProjectHealthContext context) {
    if (context.lastProgressAt() == null) {
      return 0;
    }
    int staleDays = Math.max(0, config(STALE_DAYS, 2));
    Instant staleBefore = Instant.now().minus(Duration.ofDays(staleDays));
    if (context.lastProgressAt().isBefore(staleBefore)) {
      return Math.max(0, config(STALE_PENALTY, 20));
    }
    return 0;
  }

  private int overdueTaskPenalty(ProjectHealthContext context) {
    int penalty = Math.max(0, config(OVERDUE_TASK_PENALTY, 8));
    int maxPenalty = Math.max(0, config(OVERDUE_TASK_MAX_PENALTY, 24));
    return Math.min(context.overdueTaskCount() * penalty, maxPenalty);
  }

  private String statusByScore(int score) {
    int normalScore = config(NORMAL_SCORE, 85);
    int riskScore = config(RISK_SCORE, 70);
    if (score < riskScore) {
      return RISK;
    }
    if (score < normalScore) {
      return WARNING;
    }
    return NORMAL;
  }

  private int config(String key, int defaultValue) {
    return repository.configInt(key, defaultValue);
  }

  private int clamp(int score) {
    return Math.max(0, Math.min(100, score));
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
