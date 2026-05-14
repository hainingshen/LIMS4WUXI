package com.njxs.lims.importexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.importexport.domain.ImportExportTaskRecord;
import com.njxs.lims.importexport.persistence.ImportExportTaskRepository;
import com.njxs.lims.payment.service.PaymentGateService;
import com.njxs.lims.payment.service.PaymentGateStatus;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImportExportTaskService {

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
}
