package com.njxs.lims.importexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.importexport.api.ImportExportTaskCreateRequest;
import com.njxs.lims.importexport.api.ImportExportTaskResponse;
import com.njxs.lims.importexport.domain.ImportExportTaskRecord;
import com.njxs.lims.importexport.persistence.ImportExportTaskRepository;
import com.njxs.lims.payment.service.PaymentGateService;
import com.njxs.lims.payment.service.PaymentGateStatus;
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
public class ImportExportTaskService {

  private static final Set<String> TASK_TYPES = Set.of(
      "MASTER_IMPORT",
      "MIGRATION_IMPORT",
      "LIST_EXPORT",
      "REPORT_BATCH_DOWNLOAD",
      "AUDIT_EXPORT"
  );
  private static final Set<String> IMPORT_TASK_TYPES = Set.of("MASTER_IMPORT", "MIGRATION_IMPORT");

  private final ImportExportTaskRepository importExportTaskRepository;
  private final PaymentGateService paymentGateService;
  private final ObjectMapper objectMapper;

  public ImportExportTaskService(
      ImportExportTaskRepository importExportTaskRepository,
      PaymentGateService paymentGateService,
      ObjectMapper objectMapper
  ) {
    this.importExportTaskRepository = importExportTaskRepository;
    this.paymentGateService = paymentGateService;
    this.objectMapper = objectMapper;
  }

  public PageResponse<ImportExportTaskResponse> list(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        importExportTaskRepository.list(normalizedPageSize, offset).stream()
            .map(ImportExportTaskResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        importExportTaskRepository.count()
    );
  }

  @Transactional
  public ImportExportTaskRecord create(ImportExportTaskCreateRequest request, AuthPrincipal principal) {
    String taskType = requireTaskType(request.taskType());
    Instant now = Instant.now();
    ImportExportTaskRecord record = new ImportExportTaskRecord(
        UUID.randomUUID(),
        importExportTaskRepository.nextBizNo(now.atZone(ZoneOffset.UTC).getYear()),
        taskType,
        requireText("entityType", request.entityType()).toUpperCase(Locale.ROOT),
        "PENDING",
        principal.userId(),
        now,
        request.sourceFileId(),
        null,
        null,
        null
    );
    return importExportTaskRepository.save(
        record,
        parametersJson(request.parameters() == null ? Map.of() : request.parameters()),
        principal.userId()
    );
  }

  @Transactional
  public ImportExportTaskRecord createReportBatchDownload(
      List<UUID> reportIds,
      boolean includeDeliveryLog,
      AuthPrincipal principal
  ) {
    if (reportIds == null || reportIds.isEmpty()) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("reportIds", "required")
      );
    }
    for (UUID reportId : reportIds) {
      if (paymentGateService.gateForReport(reportId) == PaymentGateStatus.LOCKED) {
        throw new BusinessException(
            "PAYMENT_GATE_LOCKED",
            "付款门禁未解锁，不能批量下载正式报告",
            HttpStatus.LOCKED,
            Map.of("reportId", reportId)
        );
      }
    }

    Instant now = Instant.now();
    ImportExportTaskRecord record = new ImportExportTaskRecord(
        UUID.randomUUID(),
        importExportTaskRepository.nextBizNo(now.atZone(ZoneOffset.UTC).getYear()),
        "REPORT_BATCH_DOWNLOAD",
        "REPORT",
        "PENDING",
        principal.userId(),
        now,
        null,
        null,
        null,
        now.plusSeconds(7 * 24 * 60 * 60)
    );
    return importExportTaskRepository.save(
        record,
        parametersJson(reportIds, includeDeliveryLog),
        principal.userId()
    );
  }

  @Transactional
  public ImportExportTaskRecord validate(UUID id, AuthPrincipal principal) {
    ImportExportTaskRecord task = task(id);
    importExportTaskRepository.updateStatus(id, "VALIDATING", null, null, null, principal.userId());
    if (IMPORT_TASK_TYPES.contains(task.taskType()) && task.sourceFileId() == null) {
      return importExportTaskRepository.updateStatus(
          id,
          "FAILED",
          null,
          "sourceFileId is required for " + task.taskType(),
          null,
          principal.userId()
      );
    }
    if (task.sourceFileId() != null && !importExportTaskRepository.existsFile(task.sourceFileId())) {
      return importExportTaskRepository.updateStatus(
          id,
          "FAILED",
          null,
          "sourceFileId does not exist: " + task.sourceFileId(),
          null,
          principal.userId()
      );
    }
    return importExportTaskRepository.updateStatus(
        id,
        "PENDING",
        null,
        "VALIDATION_PASSED",
        null,
        principal.userId()
    );
  }

  @Transactional
  public ImportExportTaskRecord run(UUID id, AuthPrincipal principal) {
    ImportExportTaskRecord task = task(id);
    if (!"PENDING".equals(task.status())) {
      throw new BusinessException(
          "IMPORT_EXPORT_TASK_STATUS_CONFLICT",
          "导入导出任务当前状态不可执行该操作",
          HttpStatus.CONFLICT,
          Map.of("expected", "PENDING", "actual", task.status())
      );
    }
    importExportTaskRepository.updateStatus(id, "RUNNING", null, null, null, principal.userId());
    UUID resultFileId = importExportTaskRepository.saveResultFileMetadata(
        task.bizNo() + "-result.json",
        "import-export/" + task.id() + "/result.json",
        "application/json",
        128,
        "sha256-" + task.id(),
        principal.userId()
    );
    return importExportTaskRepository.updateStatus(
        id,
        "SUCCESS",
        resultFileId,
        "SUCCESS: " + task.taskType() + " completed",
        Instant.now().plusSeconds(7 * 24 * 60 * 60),
        principal.userId()
    );
  }

  public ImportExportTaskRecord resultReady(UUID id) {
    ImportExportTaskRecord task = task(id);
    if (task.resultFileId() == null) {
      throw new BusinessException(
          "IMPORT_EXPORT_RESULT_NOT_READY",
          "导入导出任务结果文件尚未生成",
          HttpStatus.CONFLICT,
          Map.of("taskId", id, "status", task.status())
      );
    }
    return task;
  }

  private String parametersJson(List<UUID> reportIds, boolean includeDeliveryLog) {
    try {
      return objectMapper.writeValueAsString(Map.of(
          "reportIds", reportIds,
          "includeDeliveryLog", includeDeliveryLog
      ));
    } catch (JsonProcessingException exception) {
      throw new BusinessException(
          "IMPORT_EXPORT_PARAMETERS_INVALID",
          "导入导出任务参数不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("reason", exception.getMessage())
      );
    }
  }

  private String parametersJson(Map<String, Object> parameters) {
    try {
      return objectMapper.writeValueAsString(parameters);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(
          "IMPORT_EXPORT_PARAMETERS_INVALID",
          "导入导出任务参数不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("reason", exception.getMessage())
      );
    }
  }

  private ImportExportTaskRecord task(UUID id) {
    return importExportTaskRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "import_export_task", "entityId", id)
        ));
  }

  private String requireTaskType(String value) {
    String taskType = requireText("taskType", value).toUpperCase(Locale.ROOT);
    if (!TASK_TYPES.contains(taskType)) {
      throw new BusinessException(
          "IMPORT_EXPORT_TASK_TYPE_INVALID",
          "导入导出任务类型不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("taskType", value)
      );
    }
    return taskType;
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
}
