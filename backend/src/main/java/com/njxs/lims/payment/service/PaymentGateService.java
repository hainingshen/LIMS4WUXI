package com.njxs.lims.payment.service;

import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.payment.persistence.PaymentReceiptRepository;
import com.njxs.lims.report.domain.ReportRecord;
import com.njxs.lims.report.persistence.ReportRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentGateService {

  private final ReportRepository reportRepository;
  private final PaymentReceiptRepository paymentReceiptRepository;

  public PaymentGateService(
      ReportRepository reportRepository,
      PaymentReceiptRepository paymentReceiptRepository
  ) {
    this.reportRepository = reportRepository;
    this.paymentReceiptRepository = paymentReceiptRepository;
  }

  public PaymentGateStatus gateForReport(UUID reportId) {
    ReportRecord report = reportRepository.findById(reportId)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "report", "entityId", reportId)
        ));
    if ("GREEN_UNLOCKED".equals(report.paymentGateStatus())) {
      return PaymentGateStatus.GREEN_UNLOCKED;
    }
    if ("UNLOCKED".equals(report.paymentGateStatus())
        || paymentReceiptRepository.recordedAmountForReport(reportId).compareTo(BigDecimal.ZERO) > 0) {
      return PaymentGateStatus.UNLOCKED;
    }
    return PaymentGateStatus.LOCKED;
  }
}
