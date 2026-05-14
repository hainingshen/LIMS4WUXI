package com.njxs.lims.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.njxs.lims.report.api.ReportGenerateDraftRequest;
import com.njxs.lims.report.api.ReportResponse;
import com.njxs.lims.report.api.ReportReviewRequest;
import com.njxs.lims.report.domain.ReportRecord;
import com.njxs.lims.report.domain.ReportVersionRecord;
import com.njxs.lims.report.domain.ReportWorkOrderRecord;
import com.njxs.lims.report.persistence.ReportRepository;
import com.njxs.lims.report.snapshot.service.ReportSnapshotService;
import com.njxs.lims.report.template.domain.ReportTemplateRecord;
import com.njxs.lims.report.template.persistence.ReportTemplateRepository;
import com.njxs.lims.permission.PermissionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportService {

  private final ReportRepository reportRepository;
  private final ReportTemplateRepository reportTemplateRepository;
  private final ReportSnapshotService reportSnapshotService;
  private final ApprovalRecordService approvalRecordService;
  private final PermissionService permissionService;
  private final ReportStateMachine reportStateMachine;
  private final ProjectRepository projectRepository;
  private final ProjectStatusTransitionService projectStatusTransitionService;
  private final ObjectMapper objectMapper;

  public ReportService(
      ReportRepository reportRepository,
      ReportTemplateRepository reportTemplateRepository,
      ReportSnapshotService reportSnapshotService,
      ApprovalRecordService approvalRecordService,
      PermissionService permissionService,
      ReportStateMachine reportStateMachine,
      ProjectRepository projectRepository,
      ProjectStatusTransitionService projectStatusTransitionService,
      ObjectMapper objectMapper
  ) {
    this.reportRepository = reportRepository;
    this.reportTemplateRepository = reportTemplateRepository;
    this.reportSnapshotService = reportSnapshotService;
    this.approvalRecordService = approvalRecordService;
    this.permissionService = permissionService;
    this.reportStateMachine = reportStateMachine;
    this.projectRepository = projectRepository;
    this.projectStatusTransitionService = projectStatusTransitionService;
    this.objectMapper = objectMapper;
  }

  public PageResponse<ReportResponse> list(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        reportRepository.list(normalizedPageSize, offset).stream()
            .map(ReportResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        reportRepository.count()
    );
  }

  @Transactional
  public ReportResponse generateDraft(ReportGenerateDraftRequest request, AuthPrincipal principal) {
    String templateCode = requireText("templateCode", request.templateCode()).toUpperCase(Locale.ROOT);
    List<UUID> workOrderIds = distinctWorkOrderIds(request.workOrderIds());
    ReportTemplateRecord template = reportTemplateRepository.findPublishedByCode(templateCode)
        .orElseThrow(() -> new BusinessException(
            "REPORT_TEMPLATE_NOT_PUBLISHED",
            "报告模板未发布",
            HttpStatus.CONFLICT,
            Map.of("templateCode", templateCode)
        ));
    List<ReportWorkOrderRecord> workOrders = reportRepository.findWorkOrders(request.projectId(), workOrderIds);
    if (workOrders.size() != workOrderIds.size()) {
      throw new BusinessException(
          "REPORT_WORK_ORDER_SCOPE_INVALID",
          "报告工单不属于指定项目",
          HttpStatus.CONFLICT,
          Map.of("projectId", request.projectId(), "workOrderIds", workOrderIds)
      );
    }
    List<UUID> incompleteWorkOrders = workOrders.stream()
        .filter(workOrder -> !"COMPLETED".equals(workOrder.status()))
        .map(ReportWorkOrderRecord::workOrderId)
        .toList();
    if (!incompleteWorkOrders.isEmpty()) {
      throw new BusinessException(
          "REPORT_WORK_ORDER_NOT_COMPLETED",
          "工单未全部完成，不能生成报告草稿",
          HttpStatus.CONFLICT,
          Map.of("workOrderIds", incompleteWorkOrders)
      );
    }
    Map<UUID, UUID> rawRecordIds = rawRecordIds(workOrders);

    Instant now = Instant.now();
    ReportRecord report = new ReportRecord(
        UUID.randomUUID(),
        reportRepository.nextBizNo(now.atZone(ZoneOffset.UTC).getYear()),
        request.projectId(),
        template.id(),
        template.templateCode(),
        template.templateVersion(),
        "DRAFT",
        1,
        false,
        "DRAFT",
        "LOCKED"
    );
    ReportRecord saved = reportRepository.save(report, principal.userId());
    reportRepository.linkWorkOrders(saved.id(), rawRecordIds);

    Map<String, Object> snapshot = reportSnapshotService.buildSnapshot(saved.id(), template.templateCode());
    String snapshotJson = json(snapshot);
    UUID snapshotFileId = reportRepository.saveGeneratedFileMetadata(
        "report-snapshot-v1.json",
        "reports/" + saved.id() + "/snapshot-v1.json",
        "application/json",
        snapshotJson.getBytes(StandardCharsets.UTF_8).length,
        sha256(snapshotJson.getBytes(StandardCharsets.UTF_8)),
        principal.userId()
    );
    UUID docxFileId = reportRepository.saveGeneratedFileMetadata(
        "draft-v1.docx",
        "reports/" + saved.id() + "/draft-v1.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        0,
        sha256(new byte[0]),
        principal.userId()
    );
    reportRepository.saveVersion(new ReportVersionRecord(
        UUID.randomUUID(),
        saved.id(),
        1,
        "DRAFT",
        docxFileId,
        null,
        snapshotFileId,
        "SNAPSHOT_BUILT;DOCX_DRAFT_PLACEHOLDER"
    ), principal.userId());
    return ReportResponse.from(saved);
  }

  @Transactional
  public ReportResponse submitQc(UUID id, AuthPrincipal principal) {
    ReportRecord report = report(id);
    String nextStatus = reportStateMachine.afterSubmitQc(report);
    return ReportResponse.from(reportRepository.updateStatus(id, nextStatus, principal.userId()));
  }

  @Transactional
  public ReportResponse review(UUID id, ReportReviewRequest request, AuthPrincipal principal) {
    String stage = requireText("stage", normalize(request.stage()));
    String actionCode = reviewActionCode(stage);
    permissionService.assertAllowed(principal, actionCode, new Object[] {id, request});

    ApprovalOutcome outcome = ApprovalOutcome.require(normalize(request.outcome()));
    ReportRecord report = report(id);
    String nextStatus = reportStateMachine.afterReview(report, stage, outcome);
    approvalRecordService.record(
        "report",
        report.id(),
        report.bizNo(),
        stage,
        outcome.name(),
        request.comment(),
        principal,
        null
    );
    return ReportResponse.from(reportRepository.updateStatus(id, nextStatus, principal.userId()));
  }

  @Transactional
  public ReportResponse issue(UUID id, AuthPrincipal principal) {
    ReportRecord report = report(id);
    reportStateMachine.afterIssue(report);
    approvalRecordService.record(
        "report",
        report.id(),
        report.bizNo(),
        "ISSUE",
        ApprovalOutcome.APPROVED.name(),
        null,
        principal,
        null
    );
    ReportRecord issued = reportRepository.issue(id, principal.userId());
    reportRepository.updateCurrentVersionStatus(
        issued.id(),
        issued.currentVersionNo(),
        "ISSUED",
        principal.userId()
    );
    advanceProjectAfterIssue(issued.projectId(), principal.userId());
    return ReportResponse.from(issued);
  }

  private void advanceProjectAfterIssue(UUID projectId, UUID actorUserId) {
    Project project = projectRepository.findById(projectId).orElse(null);
    if (project == null || !ProjectStatus.REPORTING.name().equals(project.getStatus())) {
      return;
    }
    projectStatusTransitionService.transition(project, ProjectStatus.PAYMENT_HOLD, "report issued");
    projectRepository.updateStatus(project, actorUserId);
  }

  private Map<UUID, UUID> rawRecordIds(List<ReportWorkOrderRecord> workOrders) {
    Map<UUID, UUID> rawRecordIds = new LinkedHashMap<>();
    for (ReportWorkOrderRecord workOrder : workOrders) {
      if (workOrder.rawRecordId() == null) {
        throw new BusinessException(
            "REPORT_RAW_RECORD_REQUIRED",
            "工单缺少已审核通过的原始记录",
            HttpStatus.CONFLICT,
            Map.of("workOrderId", workOrder.workOrderId())
        );
      }
      rawRecordIds.put(workOrder.workOrderId(), workOrder.rawRecordId());
    }
    return rawRecordIds;
  }

  private List<UUID> distinctWorkOrderIds(List<UUID> workOrderIds) {
    if (workOrderIds == null || workOrderIds.isEmpty()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("workOrderIds", "required")
      );
    }
    List<UUID> distinct = workOrderIds.stream().distinct().toList();
    if (distinct.size() != workOrderIds.size()) {
      throw new BusinessException(
          "REPORT_WORK_ORDER_DUPLICATED",
          "报告工单重复",
          HttpStatus.BAD_REQUEST,
          Map.of("workOrderIds", workOrderIds)
      );
    }
    return distinct;
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

  private ReportRecord report(UUID id) {
    return reportRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "report", "entityId", id)
        ));
  }

  private String reviewActionCode(String stage) {
    return switch (stage) {
      case "QC" -> "REPORT_QC_REVIEW";
      case "QA" -> "REPORT_QA_REVIEW";
      case "SD" -> "REPORT_SD_REVIEW";
      default -> throw new BusinessException(
          "REPORT_REVIEW_STAGE_INVALID",
          "报告审核阶段不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("stage", stage)
      );
    };
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private String json(Map<String, Object> snapshot) {
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(
          "REPORT_SNAPSHOT_SERIALIZE_FAILED",
          "报告数据快照序列化失败",
          HttpStatus.CONFLICT,
          Map.of()
      );
    }
  }

  private String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder hex = new StringBuilder();
      for (byte value : digest) {
        hex.append("%02x".formatted(value));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
