package com.njxs.lims.project.service;

import com.njxs.lims.approval.domain.ApprovalOutcome;
import com.njxs.lims.approval.service.ApprovalRecordService;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.permission.PermissionService;
import com.njxs.lims.project.api.ProjectResponse;
import com.njxs.lims.project.domain.Project;
import com.njxs.lims.project.domain.ProjectStatus;
import com.njxs.lims.project.persistence.ProjectRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NonstandardReviewService {

  private static final String STAGE_TECH = "TECH";
  private static final String STAGE_BUSINESS = "BUSINESS";

  private final ProjectRepository projectRepository;
  private final ProjectStatusTransitionService transitionService;
  private final ApprovalRecordService approvalRecordService;
  private final PermissionService permissionService;

  public NonstandardReviewService(
      ProjectRepository projectRepository,
      ProjectStatusTransitionService transitionService,
      ApprovalRecordService approvalRecordService,
      PermissionService permissionService
  ) {
    this.projectRepository = projectRepository;
    this.transitionService = transitionService;
    this.approvalRecordService = approvalRecordService;
    this.permissionService = permissionService;
  }

  @Transactional
  public ProjectResponse start(UUID projectId, AuthPrincipal principal) {
    Project project = project(projectId);
    if (!ProjectStatus.ACCEPTED.name().equals(project.getStatus())) {
      throw statusConflict(project, ProjectStatus.ACCEPTED);
    }
    if (!project.isRequiresNonstandardReview()) {
      throw new BusinessException(
          "PROJECT_NOT_NONSTANDARD",
          "项目不需要非标评审",
          HttpStatus.CONFLICT,
          Map.of("projectId", projectId)
      );
    }
    transitionService.transition(project, ProjectStatus.NONSTANDARD_REVIEW, "start nonstandard review");
    projectRepository.updateStatus(project, principal.userId());
    return ProjectResponse.from(project);
  }

  @Transactional
  public ProjectResponse approve(
      UUID projectId,
      String stage,
      String outcome,
      String comment,
      AuthPrincipal principal
  ) {
    String normalizedStage = requireStage(stage);
    ApprovalOutcome approvalOutcome = ApprovalOutcome.require(outcome);
    if (approvalOutcome == ApprovalOutcome.NA) {
      throw new BusinessException(
          "NONSTANDARD_REVIEW_OUTCOME_INVALID",
          "非标评审结论不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("outcome", outcome)
      );
    }
    permissionService.assertAllowed(principal, actionCode(normalizedStage), new Object[] {projectId});

    Project project = project(projectId);
    if (!ProjectStatus.NONSTANDARD_REVIEW.name().equals(project.getStatus())) {
      throw statusConflict(project, ProjectStatus.NONSTANDARD_REVIEW);
    }

    approvalRecordService.record(
        "project",
        project.getId(),
        project.getBizNo(),
        normalizedStage,
        approvalOutcome.name(),
        comment,
        principal,
        null
    );
    if (approvalOutcome == ApprovalOutcome.REJECTED) {
      transitionService.transition(project, ProjectStatus.ACCEPTED, "nonstandard review rejected");
      projectRepository.updateStatus(project, principal.userId());
      return ProjectResponse.from(project);
    }
    if (approvalRecordService.hasApproved("project", project.getId(), STAGE_TECH)
        && approvalRecordService.hasApproved("project", project.getId(), STAGE_BUSINESS)) {
      transitionService.transition(project, ProjectStatus.QUOTABLE, "nonstandard review approved");
      projectRepository.updateStatus(project, principal.userId());
    }
    return ProjectResponse.from(project);
  }

  private Project project(UUID projectId) {
    return projectRepository.findById(projectId)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "project", "entityId", projectId)
        ));
  }

  private String requireStage(String stage) {
    if (STAGE_TECH.equals(stage) || STAGE_BUSINESS.equals(stage)) {
      return stage;
    }
    throw new BusinessException(
        "NONSTANDARD_REVIEW_STAGE_INVALID",
        "非标评审阶段不合法",
        HttpStatus.BAD_REQUEST,
        Map.of("stage", stage == null ? "" : stage)
    );
  }

  private String actionCode(String stage) {
    return STAGE_TECH.equals(stage) ? "NONSTANDARD_TECH_APPROVE" : "NONSTANDARD_BUSINESS_APPROVE";
  }

  private BusinessException statusConflict(Project project, ProjectStatus expected) {
    return new BusinessException(
        "PROJECT_STATUS_CONFLICT",
        "项目当前状态不可执行该操作",
        HttpStatus.CONFLICT,
        Map.of("expected", expected.name(), "actual", project.getStatus())
    );
  }
}
