package com.njxs.lims.project.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.domain.BaseEntity;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.project.api.ProjectResponse;
import com.njxs.lims.project.domain.Project;
import com.njxs.lims.project.domain.ProjectStatus;
import com.njxs.lims.project.persistence.ProjectRepository;
import com.njxs.lims.submission.api.SubmissionSplitProjectsRequest;
import com.njxs.lims.submission.persistence.SubmissionRecord;
import com.njxs.lims.submission.persistence.SubmissionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProjectSplitService {

  private final SubmissionRepository submissionRepository;
  private final ProjectRepository projectRepository;

  public ProjectSplitService(SubmissionRepository submissionRepository, ProjectRepository projectRepository) {
    this.submissionRepository = submissionRepository;
    this.projectRepository = projectRepository;
  }

  @Transactional
  public List<ProjectResponse> splitFromSubmission(
      UUID submissionId,
      SubmissionSplitProjectsRequest request,
      AuthPrincipal principal
  ) {
    SubmissionRecord submission = submissionRepository.findById(submissionId)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "submission", "entityId", submissionId)
        ));
    if (!"ACCEPTED".equals(submission.status())) {
      throw new BusinessException(
          "SUBMISSION_STATUS_CONFLICT",
          "送检单当前状态不可拆分项目",
          HttpStatus.CONFLICT,
          Map.of("expected", "ACCEPTED", "actual", submission.status())
      );
    }

    List<Map<String, Object>> drafts = projectDrafts(request);
    Instant now = Instant.now();
    int year = Year.now().getValue();
    List<Project> projects = new ArrayList<>();
    for (Map<String, Object> draft : drafts) {
      Project project = project(submission, draft, principal.userId(), now, year);
      projectRepository.save(project);
      projects.add(project);
    }
    submissionRepository.markProjectSplit(submissionId, principal.userId());
    return projects.stream().map(ProjectResponse::from).toList();
  }

  private Project project(
      SubmissionRecord submission,
      Map<String, Object> draft,
      UUID actorUserId,
      Instant now,
      int year
  ) {
    UUID projectId = UUID.randomUUID();
    String projectType = text(draft, "projectType", "STD_ACCREDITED");
    boolean requiresAnimalTest = bool(draft, "requiresAnimalTest", false);
    boolean requiresEthicsReview = bool(draft, "requiresEthicsReview", requiresAnimalTest);
    boolean requiresNonstandardReview = bool(
        draft,
        "requiresNonstandardReview",
        "NON_STANDARD".equals(projectType)
    );

    Project project = new Project();
    base(project, projectId, projectRepository.nextProjectBizNo(year), actorUserId, submission.customerOrgId(), now);
    project.setSubmissionId(submission.id());
    project.setCustomerOrgId(submission.customerOrgId());
    project.setProjectType(projectType);
    project.setQualificationType(text(draft, "qualificationType", "CMA"));
    project.setServiceType(text(draft, "serviceType", "NORMAL"));
    project.setUrgentExtraFeePercent(decimal(draft, "urgentExtraFeePercent"));
    project.setUrgentTatDays(integer(draft, "urgentTatDays"));
    project.setRequiresGlp(submission.requiresGlp());
    project.setRequiresAnimalTest(requiresAnimalTest);
    project.setRequiresEthicsReview(requiresEthicsReview);
    project.setRequiresNonstandardReview(requiresNonstandardReview);
    project.setBusinessOwnerId(defaultOwner("BUSINESS_OWNER", actorUserId));
    project.setTechnicalOwnerId(defaultOwner("TECHNICAL_LEADER", actorUserId));
    project.setSdUserId(actorUserId);
    project.setStatus(ProjectStatus.ACCEPTED.name());
    project.setHealthStatus("NORMAL");
    project.setHealthScore(100);
    project.setPaymentStatus("UNPAID");
    return project;
  }

  private List<Map<String, Object>> projectDrafts(SubmissionSplitProjectsRequest request) {
    if (request == null || request.projectDrafts() == null || request.projectDrafts().isEmpty()) {
      return List.of(Map.of());
    }
    return request.projectDrafts();
  }

  private UUID defaultOwner(String roleCode, UUID fallbackUserId) {
    return projectRepository.findFirstActiveUserIdByRole(roleCode).orElse(fallbackUserId);
  }

  private void base(BaseEntity entity, UUID id, String bizNo, UUID actorUserId, UUID tenantOrgId, Instant now) {
    entity.setId(id);
    entity.setBizNo(bizNo);
    entity.setCreatedAt(now);
    entity.setCreatedBy(actorUserId);
    entity.setUpdatedAt(now);
    entity.setUpdatedBy(actorUserId);
    entity.setTenantOrgId(tenantOrgId);
    entity.setSourceType("NEW");
    entity.setDataQualityScore(100);
  }

  private String text(Map<String, Object> values, String field, String defaultValue) {
    Object value = values.get(field);
    if (value == null) {
      return defaultValue;
    }
    String text = value.toString();
    return text.isBlank() ? defaultValue : text;
  }

  private boolean bool(Map<String, Object> values, String field, boolean defaultValue) {
    Object value = values.get(field);
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    if (value == null) {
      return defaultValue;
    }
    String text = value.toString();
    return "true".equalsIgnoreCase(text) || "YES".equalsIgnoreCase(text);
  }

  private BigDecimal decimal(Map<String, Object> values, String field) {
    Object value = values.get(field);
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return new BigDecimal(value.toString());
  }

  private Integer integer(Map<String, Object> values, String field) {
    Object value = values.get(field);
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return Integer.valueOf(value.toString());
  }
}
