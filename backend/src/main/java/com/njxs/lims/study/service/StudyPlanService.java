package com.njxs.lims.study.service;

import com.njxs.lims.approval.service.ApprovalRecordService;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.project.domain.Project;
import com.njxs.lims.project.persistence.ProjectRepository;
import com.njxs.lims.sample.persistence.SampleRepository;
import com.njxs.lims.study.api.StudyPlanCreateRequest;
import com.njxs.lims.study.api.StudyPlanResponse;
import com.njxs.lims.study.domain.StudyPlanRecord;
import com.njxs.lims.study.persistence.StudyPlanRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StudyPlanService {

  private static final Set<String> REVIEWABLE_STATUSES = Set.of("DRAFT", "REVIEWING");

  private final StudyPlanRepository studyPlanRepository;
  private final ProjectRepository projectRepository;
  private final SampleRepository sampleRepository;
  private final ApprovalRecordService approvalRecordService;

  public StudyPlanService(
      StudyPlanRepository studyPlanRepository,
      ProjectRepository projectRepository,
      SampleRepository sampleRepository,
      ApprovalRecordService approvalRecordService
  ) {
    this.studyPlanRepository = studyPlanRepository;
    this.projectRepository = projectRepository;
    this.sampleRepository = sampleRepository;
    this.approvalRecordService = approvalRecordService;
  }

  @Transactional
  public StudyPlanResponse create(StudyPlanCreateRequest request, AuthPrincipal principal) {
    Project project = project(request.projectId());
    Instant now = Instant.now();
    StudyPlanRecord record = new StudyPlanRecord(
        UUID.randomUUID(),
        studyPlanRepository.nextPlanBizNo(now.atZone(ZoneOffset.UTC).getYear()),
        request.projectId(),
        "DRAFT",
        request.requiresConditioning(),
        project.isRequiresGlp(),
        project.isRequiresAnimalTest(),
        request.plannedStartAt(),
        request.plannedFinishAt(),
        null,
        null,
        null
    );
    return StudyPlanResponse.from(studyPlanRepository.save(record, principal.userId()));
  }

  public PageResponse<StudyPlanResponse> list(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        studyPlanRepository.list(normalizedPageSize, offset).stream()
            .map(StudyPlanResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        studyPlanRepository.count()
    );
  }

  @Transactional
  public StudyPlanResponse submitReview(UUID id, AuthPrincipal principal) {
    StudyPlanRecord plan = studyPlan(id);
    requireStatus(plan, REVIEWABLE_STATUSES);
    Instant now = Instant.now();
    approvalRecordService.record(
        "study_plan",
        plan.id(),
        plan.bizNo(),
        "SD",
        "APPROVED",
        "SD approved",
        principal,
        null
    );
    DispatchReadiness readiness = readiness(plan, true);
    StudyPlanRecord reviewed = studyPlanRepository.markReviewed(
        id,
        readiness.ready() ? "READY_TO_DISPATCH" : "PENDING_DISPATCH",
        now,
        principal.userId()
    );
    return StudyPlanResponse.from(reviewed);
  }

  @Transactional
  public StudyPlanResponse dispatch(UUID id, AuthPrincipal principal) {
    StudyPlanRecord plan = studyPlan(id);
    DispatchReadiness readiness = readiness(plan);
    if (!readiness.ready()) {
      studyPlanRepository.updateStatus(id, "PENDING_DISPATCH", principal.userId());
      throw new BusinessException(
          "STUDY_PLAN_NOT_READY_TO_DISPATCH",
          "试验计划资料未完备，不能下发工单",
          HttpStatus.CONFLICT,
          Map.of(
              "contractSigned", readiness.contractSigned(),
              "samplesReady", readiness.samplesReady(),
              "requiredApprovalsDone", readiness.requiredApprovalsDone(),
              "conditioningDone", readiness.conditioningDone()
          )
      );
    }
    if (!"READY_TO_DISPATCH".equals(plan.status()) && !"PENDING_DISPATCH".equals(plan.status())) {
      throw new BusinessException(
          "STUDY_PLAN_STATUS_CONFLICT",
          "试验计划当前状态不可执行该操作",
          HttpStatus.CONFLICT,
          Map.of("expected", Set.of("READY_TO_DISPATCH", "PENDING_DISPATCH"), "actual", plan.status())
      );
    }
    if (studyPlanRepository.workOrderCount(id) == 0) {
      studyPlanRepository.createWorkOrders(id, principal.userId());
    }
    return StudyPlanResponse.from(studyPlanRepository.updateStatus(id, "DISPATCHED", principal.userId()));
  }

  private DispatchReadiness readiness(StudyPlanRecord plan) {
    return readiness(plan, false);
  }

  private DispatchReadiness readiness(StudyPlanRecord plan, boolean sdApprovedInCurrentAction) {
    return new DispatchReadiness(
        studyPlanRepository.signedContractExists(plan.projectId()),
        sampleRepository.allProjectSamplesInStock(plan.projectId()),
        requiredApprovalsDone(plan, sdApprovedInCurrentAction),
        !plan.requiresConditioning()
    );
  }

  private boolean requiredApprovalsDone(StudyPlanRecord plan, boolean sdApprovedInCurrentAction) {
    return (sdApprovedInCurrentAction || plan.sdApprovedAt() != null)
        && (!plan.requiresGlp() || plan.qaApprovedAt() != null)
        && (!plan.requiresAnimalTest() || plan.ethicsApprovedAt() != null);
  }

  private StudyPlanRecord studyPlan(UUID id) {
    return studyPlanRepository.findById(id)
        .orElseThrow(() -> notFound("study_plan", id));
  }

  private Project project(UUID projectId) {
    return projectRepository.findById(projectId)
        .orElseThrow(() -> notFound("project", projectId));
  }

  private void requireStatus(StudyPlanRecord plan, Set<String> expected) {
    if (!expected.contains(plan.status())) {
      throw new BusinessException(
          "STUDY_PLAN_STATUS_CONFLICT",
          "试验计划当前状态不可执行该操作",
          HttpStatus.CONFLICT,
          Map.of("expected", expected, "actual", plan.status())
      );
    }
  }

  private BusinessException notFound(String entityType, UUID entityId) {
    return new BusinessException(
        "NOT_FOUND",
        "资源不存在",
        HttpStatus.NOT_FOUND,
        Map.of("entityType", entityType, "entityId", entityId)
    );
  }

  private record DispatchReadiness(
      boolean contractSigned,
      boolean samplesReady,
      boolean requiredApprovalsDone,
      boolean conditioningDone
  ) {

    boolean ready() {
      return contractSigned && samplesReady && requiredApprovalsDone && conditioningDone;
    }
  }
}
