package com.njxs.lims.payment.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.payment.api.PaymentReceiptCreateRequest;
import com.njxs.lims.payment.api.PaymentReceiptResponse;
import com.njxs.lims.payment.domain.PaymentReceiptRecord;
import com.njxs.lims.payment.persistence.PaymentReceiptRepository;
import java.time.ZoneOffset;
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
public class PaymentReceiptService {

  private static final Set<String> METHODS = Set.of("BANK_TRANSFER", "CASH", "OTHER");

  private final PaymentReceiptRepository paymentReceiptRepository;

  public PaymentReceiptService(PaymentReceiptRepository paymentReceiptRepository) {
    this.paymentReceiptRepository = paymentReceiptRepository;
  }

  public PageResponse<PaymentReceiptResponse> list(UUID projectId, int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        paymentReceiptRepository.list(projectId, normalizedPageSize, offset).stream()
            .map(PaymentReceiptResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        paymentReceiptRepository.count(projectId)
    );
  }

  @Transactional
  public PaymentReceiptResponse create(PaymentReceiptCreateRequest request, AuthPrincipal principal) {
    if (!paymentReceiptRepository.projectExists(request.projectId())) {
      throw notFound("project", request.projectId());
    }
    if (request.reportId() != null
        && !paymentReceiptRepository.reportBelongsToProject(request.reportId(), request.projectId())) {
      throw new BusinessException(
          "PAYMENT_REPORT_SCOPE_INVALID",
          "收款报告不属于指定项目",
          HttpStatus.CONFLICT,
          Map.of("projectId", request.projectId(), "reportId", request.reportId())
      );
    }
    String method = normalizeMethod(request.method());
    PaymentReceiptRecord record = new PaymentReceiptRecord(
        UUID.randomUUID(),
        paymentReceiptRepository.nextBizNo(request.receivedAt().atZone(ZoneOffset.UTC).getYear()),
        request.projectId(),
        request.reportId(),
        request.amount(),
        request.receivedAt(),
        principal.userId(),
        method,
        request.voucherFileId(),
        emptyToNull(request.note()),
        "RECORDED"
    );
    PaymentReceiptRecord saved = paymentReceiptRepository.save(record, principal.userId());
    paymentReceiptRepository.markProjectPaid(saved.projectId(), principal.userId());
    if (saved.reportId() == null) {
      paymentReceiptRepository.unlockReportsByProject(saved.projectId(), principal.userId());
    } else {
      paymentReceiptRepository.unlockReport(saved.reportId(), principal.userId());
    }
    return PaymentReceiptResponse.from(saved);
  }

  private String normalizeMethod(String method) {
    String normalized = method == null ? null : method.trim().toUpperCase(Locale.ROOT);
    if (!METHODS.contains(normalized)) {
      throw new BusinessException(
          "PAYMENT_METHOD_INVALID",
          "收款方式不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("method", method)
      );
    }
    return normalized;
  }

  private BusinessException notFound(String entityType, UUID entityId) {
    return new BusinessException(
        "NOT_FOUND",
        "资源不存在",
        HttpStatus.NOT_FOUND,
        Map.of("entityType", entityType, "entityId", entityId)
    );
  }

  private String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
