package com.njxs.lims.study.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.approval.domain.ApprovalOutcome;
import com.njxs.lims.approval.service.ApprovalRecordService;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.project.domain.Project;
import com.njxs.lims.project.domain.ProjectStatus;
import com.njxs.lims.project.persistence.ProjectRepository;
import com.njxs.lims.project.service.ProjectStatusTransitionService;
import com.njxs.lims.study.api.RawRecordResponse;
import com.njxs.lims.study.api.RawRecordReviewRequest;
import com.njxs.lims.study.api.RawRecordSubmitRequest;
import com.njxs.lims.study.domain.RawRecordRecord;
import com.njxs.lims.study.domain.WorkOrderRecord;
import com.njxs.lims.study.persistence.RawRecordRepository;
import com.njxs.lims.study.persistence.WorkOrderRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RawRecordService {

  private final RawRecordRepository rawRecordRepository;
  private final WorkOrderRepository workOrderRepository;
  private final ProjectRepository projectRepository;
  private final ProjectStatusTransitionService projectStatusTransitionService;
  private final ApprovalRecordService approvalRecordService;
  private final ObjectMapper objectMapper;

  public RawRecordService(
      RawRecordRepository rawRecordRepository,
      WorkOrderRepository workOrderRepository,
      ProjectRepository projectRepository,
      ProjectStatusTransitionService projectStatusTransitionService,
      ApprovalRecordService approvalRecordService,
      ObjectMapper objectMapper
  ) {
    this.rawRecordRepository = rawRecordRepository;
    this.workOrderRepository = workOrderRepository;
    this.projectRepository = projectRepository;
    this.projectStatusTransitionService = projectStatusTransitionService;
    this.approvalRecordService = approvalRecordService;
    this.objectMapper = objectMapper;
  }

  public PageResponse<RawRecordResponse> list(UUID workOrderId, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        rawRecordRepository.list(workOrderId, normalizedPageSize, offset).stream()
            .map(RawRecordResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        rawRecordRepository.count(workOrderId)
    );
  }

  @Transactional
  public RawRecordResponse submit(UUID workOrderId, RawRecordSubmitRequest request, AuthPrincipal principal) {
    WorkOrderRecord workOrder = workOrder(workOrderId);
    requireWorkOrderStatus(workOrder, Set.of("RESULT_ENTERED"));

    JsonNode recordData = requireRecordData(request.recordData());
    Instant now = Instant.now();
    RawRecordRecord record = new RawRecordRecord(
        UUID.randomUUID(),
        rawRecordRepository.nextBizNo(now.atZone(ZoneOffset.UTC).getYear()),
        workOrder.id(),
        workOrder.projectId(),
        workOrder.sampleId(),
        requireText("templateCode", request.templateCode()),
        requiredField(recordData, "testItem"),
        requiredField(recordData, "testBasis"),
        optionalField(recordData, "testTime"),
        optionalField(recordData, "testLocation"),
        optionalField(recordData, "temperatureCelsius"),
        optionalField(recordData, "humidityPercent"),
        optionalField(recordData, "method"),
        json(recordData),
        json(request.attachmentFileIds() == null ? List.of() : request.attachmentFileIds()),
        "REVIEWING",
        principal.userId(),
        null,
        now,
        null
    );
    RawRecordRecord saved = rawRecordRepository.save(record);
    workOrderRepository.updateStatus(workOrder.id(), "RAW_REVIEWING", 90, principal.userId());
    return RawRecordResponse.from(saved);
  }

  @Transactional
  public RawRecordResponse review(UUID id, RawRecordReviewRequest request, AuthPrincipal principal) {
    RawRecordRecord record = rawRecord(id);
    requireRawRecordStatus(record, Set.of("REVIEWING"));

    String stage = requireText("stage", normalize(request.stage()));
    if (!"QC".equals(stage)) {
      throw new BusinessException(
          "RAW_RECORD_STAGE_INVALID",
          "原始记录审核阶段不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("expected", "QC", "actual", stage)
      );
    }
    ApprovalOutcome outcome = ApprovalOutcome.require(normalize(request.outcome()));
    if (outcome == ApprovalOutcome.NA) {
      throw new BusinessException(
          "RAW_RECORD_OUTCOME_INVALID",
          "原始记录审核结论不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("outcome", request.outcome())
      );
    }

    approvalRecordService.record(
        "raw_record",
        record.id(),
        record.bizNo(),
        stage,
        outcome.name(),
        request.comment(),
        principal,
        null
    );
    RawRecordRecord reviewed = rawRecordRepository.updateReview(
        id,
        outcome == ApprovalOutcome.APPROVED ? "APPROVED" : "REJECTED",
        principal.userId(),
        Instant.now(),
        principal.userId()
    );
    if (outcome == ApprovalOutcome.REJECTED) {
      workOrderRepository.updateStatus(record.workOrderId(), "RESULT_ENTERED", 80, principal.userId());
    } else {
      workOrderRepository.updateStatus(record.workOrderId(), "COMPLETED", 100, principal.userId());
      advanceProjectIfReady(record.projectId(), principal.userId());
    }
    return RawRecordResponse.from(reviewed);
  }

  private void advanceProjectIfReady(UUID projectId, UUID actorUserId) {
    if (!workOrderRepository.allCompletedByProject(projectId)) {
      return;
    }
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> notFound("project", projectId));
    if (!ProjectStatus.IN_TESTING.name().equals(project.getStatus())) {
      return;
    }
    projectStatusTransitionService.transition(
        project,
        ProjectStatus.REPORTING,
        "ALL_REQUIRED_WORK_ORDERS_COMPLETED"
    );
    projectRepository.updateStatus(project, actorUserId);
  }

  private WorkOrderRecord workOrder(UUID id) {
    return workOrderRepository.findById(id)
        .orElseThrow(() -> notFound("work_order", id));
  }

  private RawRecordRecord rawRecord(UUID id) {
    return rawRecordRepository.findById(id)
        .orElseThrow(() -> notFound("raw_record", id));
  }

  private void requireWorkOrderStatus(WorkOrderRecord record, Set<String> expected) {
    if (!expected.contains(record.status())) {
      throw new BusinessException(
          "WORK_ORDER_STATUS_CONFLICT",
          "工单当前状态不可执行该操作",
          HttpStatus.CONFLICT,
          Map.of("expected", expected, "actual", record.status())
      );
    }
  }

  private void requireRawRecordStatus(RawRecordRecord record, Set<String> expected) {
    if (!expected.contains(record.status())) {
      throw new BusinessException(
          "RAW_RECORD_STATUS_CONFLICT",
          "原始记录当前状态不可执行该操作",
          HttpStatus.CONFLICT,
          Map.of("expected", expected, "actual", record.status())
      );
    }
  }

  private JsonNode requireRecordData(JsonNode recordData) {
    if (recordData == null || recordData.isNull() || !recordData.isObject()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("recordData", "object required")
      );
    }
    return recordData;
  }

  private String requiredField(JsonNode data, String field) {
    return requireText("recordData." + field, optionalField(data, field));
  }

  private String optionalField(JsonNode data, String field) {
    String value = data.path(field).asText(null);
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String requireText(String field, String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "required")
      );
    }
    return value.trim();
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("json", "invalid")
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
}
