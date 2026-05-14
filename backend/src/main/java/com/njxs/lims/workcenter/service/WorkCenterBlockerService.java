package com.njxs.lims.workcenter.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.workcenter.api.WorkCenterBlockerCloseRequest;
import com.njxs.lims.workcenter.api.WorkCenterBlockerCreateRequest;
import com.njxs.lims.workcenter.api.WorkCenterBlockerResponse;
import com.njxs.lims.workcenter.api.WorkCenterBlockerUpdateRequest;
import com.njxs.lims.workcenter.domain.WorkCenterBlockerRecord;
import com.njxs.lims.workcenter.domain.WorkCenterScope;
import com.njxs.lims.workcenter.persistence.WorkCenterBlockerRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkCenterBlockerService {

  private static final Set<String> BLOCKER_TYPES = Set.of(
      "PENDING_APPROVAL",
      "MISSING_SAMPLE",
      "MISSING_MATERIAL",
      "DEVICE_CONFLICT",
      "STAFF_UNAVAILABLE",
      "TECHNICAL_RISK",
      "CUSTOMER_CONFIRMATION",
      "SUBCONTRACT_DELAY",
      "PAYMENT_BLOCKED",
      "REPORT_REWORK",
      "OTHER"
  );
  private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH");
  private static final Set<String> UPDATE_STATUSES = Set.of("OPEN", "IN_PROGRESS", "RESOLVED");

  private final WorkCenterBlockerRepository blockerRepository;
  private final ProjectHealthService projectHealthService;

  public WorkCenterBlockerService(
      WorkCenterBlockerRepository blockerRepository,
      ProjectHealthService projectHealthService
  ) {
    this.blockerRepository = blockerRepository;
    this.projectHealthService = projectHealthService;
  }

  public PageResponse<WorkCenterBlockerResponse> list(AuthPrincipal principal, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    WorkCenterScope scope = WorkCenterScope.from(principal);
    return new PageResponse<>(
        blockerRepository.list(scope, normalizedPageSize, offset).stream()
            .map(WorkCenterBlockerResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        blockerRepository.count(scope)
    );
  }

  @Transactional
  public WorkCenterBlockerResponse create(WorkCenterBlockerCreateRequest request, AuthPrincipal principal) {
    requireProject(request.projectId());
    WorkCenterBlockerRecord record = new WorkCenterBlockerRecord(
        UUID.randomUUID(),
        request.projectId(),
        allowed(request.blockerType(), BLOCKER_TYPES, "blockerType"),
        allowed(request.severity(), SEVERITIES, "severity"),
        required(request.description(), "description"),
        required(request.impactScope(), "impactScope"),
        request.expectedRecoveryAt(),
        request.ownerUserId(),
        collaborators(request.collaboratorIds()),
        "OPEN",
        null
    );
    WorkCenterBlockerRecord created = blockerRepository.insert(record, principal.userId());
    projectHealthService.recalculate(created.projectId());
    return WorkCenterBlockerResponse.from(created);
  }

  @Transactional
  public WorkCenterBlockerResponse update(UUID id, WorkCenterBlockerUpdateRequest request, AuthPrincipal principal) {
    WorkCenterBlockerRecord current = blocker(id);
    requireOpenForMutation(current);
    if ("CLOSED".equals(request.status())) {
      throw resolutionRequired();
    }
    String status = request.status() == null
        ? current.status()
        : allowed(request.status(), UPDATE_STATUSES, "status");
    WorkCenterBlockerRecord updated = blockerRepository.update(new WorkCenterBlockerRecord(
        current.id(),
        current.projectId(),
        request.blockerType() == null ? current.blockerType() : allowed(request.blockerType(), BLOCKER_TYPES, "blockerType"),
        request.severity() == null ? current.severity() : allowed(request.severity(), SEVERITIES, "severity"),
        request.description() == null ? current.description() : required(request.description(), "description"),
        request.impactScope() == null ? current.impactScope() : required(request.impactScope(), "impactScope"),
        request.expectedRecoveryAt() == null ? current.expectedRecoveryAt() : request.expectedRecoveryAt(),
        request.ownerUserId() == null ? current.ownerUserId() : request.ownerUserId(),
        request.collaboratorIds() == null ? current.collaboratorIds() : collaborators(request.collaboratorIds()),
        status,
        current.resolution()
    ), principal.userId());
    projectHealthService.recalculate(updated.projectId());
    return WorkCenterBlockerResponse.from(updated);
  }

  @Transactional
  public WorkCenterBlockerResponse close(UUID id, WorkCenterBlockerCloseRequest request, AuthPrincipal principal) {
    String resolution = request == null ? null : request.resolution();
    if (resolution == null || resolution.isBlank()) {
      throw resolutionRequired();
    }
    WorkCenterBlockerRecord current = blocker(id);
    requireOpenForMutation(current);
    WorkCenterBlockerRecord closed = blockerRepository.update(new WorkCenterBlockerRecord(
        current.id(),
        current.projectId(),
        current.blockerType(),
        current.severity(),
        current.description(),
        current.impactScope(),
        current.expectedRecoveryAt(),
        current.ownerUserId(),
        current.collaboratorIds(),
        "CLOSED",
        resolution.strip()
    ), principal.userId());
    projectHealthService.recalculate(closed.projectId());
    return WorkCenterBlockerResponse.from(closed);
  }

  private void requireProject(UUID projectId) {
    if (!blockerRepository.projectExists(projectId)) {
      throw notFound("project", projectId);
    }
  }

  private WorkCenterBlockerRecord blocker(UUID id) {
    return blockerRepository.findById(id)
        .orElseThrow(() -> notFound("work_center_blocker", id));
  }

  private void requireOpenForMutation(WorkCenterBlockerRecord record) {
    if ("CLOSED".equals(record.status())) {
      throw new BusinessException(
          "BLOCKER_STATUS_CONFLICT",
          "阻塞事项已关闭，不能继续修改",
          HttpStatus.CONFLICT,
          Map.of("id", record.id(), "status", record.status())
      );
    }
  }

  private String allowed(String value, Set<String> allowed, String field) {
    String normalized = required(value, field).toUpperCase();
    if (!allowed.contains(normalized)) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, value)
      );
    }
    return normalized;
  }

  private String required(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "required")
      );
    }
    return value.strip();
  }

  private List<UUID> collaborators(List<UUID> collaboratorIds) {
    return collaboratorIds == null ? List.of() : List.copyOf(collaboratorIds);
  }

  private BusinessException resolutionRequired() {
    return new BusinessException(
        "BLOCKER_RESOLUTION_REQUIRED",
        "关闭阻塞事项必须填写解决说明",
        HttpStatus.BAD_REQUEST,
        Map.of("resolution", "required")
    );
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
