package com.njxs.lims.report.delivery.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.payment.service.PaymentGateStatus;
import com.njxs.lims.report.delivery.api.ReportDeliveryLogResponse;
import com.njxs.lims.report.delivery.domain.ReportDeliveryLogRecord;
import com.njxs.lims.report.delivery.persistence.ReportDeliveryLogRepository;
import java.time.Instant;
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
public class ReportDeliveryLogService {

  private static final Set<String> DELIVERY_TYPES = Set.of(
      "PREVIEW",
      "DOWNLOAD",
      "EMAIL",
      "BATCH_DOWNLOAD",
      "GREEN_CHANNEL"
  );

  private final ReportDeliveryLogRepository reportDeliveryLogRepository;

  public ReportDeliveryLogService(ReportDeliveryLogRepository reportDeliveryLogRepository) {
    this.reportDeliveryLogRepository = reportDeliveryLogRepository;
  }

  public PageResponse<ReportDeliveryLogResponse> list(UUID reportId, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        reportDeliveryLogRepository.list(reportId, normalizedPageSize, offset).stream()
            .map(ReportDeliveryLogResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        reportDeliveryLogRepository.count(reportId)
    );
  }

  @Transactional
  public ReportDeliveryLogRecord recordForCurrentVersion(
      UUID reportId,
      String deliveryType,
      AuthPrincipal principal,
      PaymentGateStatus paymentGateStatus,
      UUID fileId
  ) {
    UUID reportVersionId = reportDeliveryLogRepository.currentVersionId(reportId)
        .orElseThrow(() -> new BusinessException(
            "REPORT_VERSION_NOT_FOUND",
            "报告版本不存在",
            HttpStatus.CONFLICT,
            Map.of("reportId", reportId)
        ));
    return record(new DeliveryLogCommand(
        reportId,
        reportVersionId,
        deliveryType,
        null,
        principal.userId(),
        principal.clientType(),
        paymentGateStatus,
        fileId
    ));
  }

  @Transactional
  public ReportDeliveryLogRecord record(DeliveryLogCommand command) {
    requireUuid("reportId", command.reportId());
    requireUuid("reportVersionId", command.reportVersionId());
    String deliveryType = normalize(command.deliveryType());
    if (!DELIVERY_TYPES.contains(deliveryType)) {
      throw new BusinessException(
          "REPORT_DELIVERY_TYPE_INVALID",
          "报告发放类型不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("deliveryType", String.valueOf(command.deliveryType()))
      );
    }
    if (command.clientType() == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("clientType", "required")
      );
    }
    if (command.paymentGateStatus() == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of("paymentGateStatus", "required")
      );
    }
    ReportDeliveryLogRecord record = new ReportDeliveryLogRecord(
        UUID.randomUUID(),
        command.reportId(),
        command.reportVersionId(),
        deliveryType,
        emptyToNull(command.deliveredTo()),
        command.deliveredBy(),
        Instant.now(),
        command.clientType().name(),
        command.paymentGateStatus().name(),
        command.fileId()
    );
    return reportDeliveryLogRepository.save(record, command.deliveredBy());
  }

  private void requireUuid(String field, UUID value) {
    if (value == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "required")
      );
    }
  }

  private String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }
}
