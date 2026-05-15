package com.njxs.lims.report.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.file.domain.StoredFile;
import com.njxs.lims.payment.service.PaymentGateService;
import com.njxs.lims.payment.service.PaymentGateStatus;
import com.njxs.lims.permission.PermissionService;
import com.njxs.lims.report.conversion.DocumentConversionService;
import com.njxs.lims.report.delivery.service.ReportDeliveryLogService;
import com.njxs.lims.report.domain.ReportRecord;
import com.njxs.lims.report.domain.ReportVersionRecord;
import com.njxs.lims.report.persistence.ReportRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportPdfService {

  private static final Set<String> SIGNED_STATUSES = Set.of(
      "ISSUED_PAYMENT_LOCKED",
      "GREEN_RELEASED",
      "RELEASED"
  );

  private final ReportRepository reportRepository;
  private final DocumentConversionService documentConversionService;
  private final PaymentGateService paymentGateService;
  private final ReportDeliveryLogService reportDeliveryLogService;
  private final PermissionService permissionService;

  public ReportPdfService(
      ReportRepository reportRepository,
      DocumentConversionService documentConversionService,
      PaymentGateService paymentGateService,
      ReportDeliveryLogService reportDeliveryLogService,
      PermissionService permissionService
  ) {
    this.reportRepository = reportRepository;
    this.documentConversionService = documentConversionService;
    this.paymentGateService = paymentGateService;
    this.reportDeliveryLogService = reportDeliveryLogService;
    this.permissionService = permissionService;
  }

  @Transactional
  public StoredFile downloadOfficialPdf(UUID reportId, AuthPrincipal principal) {
    permissionService.assertAllowed(principal, "REPORT_OFFICIAL_DOWNLOAD", new Object[] {reportId});
    PaymentGateStatus gateStatus = paymentGateService.gateForReport(reportId);
    if (gateStatus == PaymentGateStatus.LOCKED) {
      throw new BusinessException("PAYMENT_GATE_LOCKED");
    }
    StoredFile pdf = generateOfficialPdf(reportId, principal);
    reportDeliveryLogService.recordForCurrentVersion(
        reportId,
        deliveryType(principal),
        principal,
        gateStatus,
        pdf.fileId()
    );
    return pdf;
  }

  @Transactional
  public StoredFile generateOfficialPdf(UUID reportId, AuthPrincipal principal) {
    ReportRecord report = report(reportId);
    if (!SIGNED_STATUSES.contains(report.status())) {
      throw new BusinessException(
          "REPORT_NOT_ISSUED",
          "报告未签发，不能生成正式PDF",
          HttpStatus.CONFLICT,
          Map.of("reportId", reportId, "status", report.status())
      );
    }

    ReportVersionRecord version = reportRepository.findVersion(report.id(), report.currentVersionNo())
        .orElseThrow(() -> new BusinessException(
            "REPORT_VERSION_NOT_FOUND",
            "报告版本不存在",
            HttpStatus.CONFLICT,
            Map.of("reportId", report.id(), "versionNo", report.currentVersionNo())
        ));
    if (version.pdfFileId() != null) {
      return new StoredFile(version.pdfFileId(), null, null, "application/pdf", 0, null);
    }
    if (version.docxFileId() == null) {
      throw new BusinessException(
          "REPORT_DOCX_REQUIRED",
          "报告版本缺少DOCX文件，不能转换PDF",
          HttpStatus.CONFLICT,
          Map.of("reportId", report.id(), "versionNo", report.currentVersionNo())
      );
    }

    StoredFile pdf = documentConversionService.convertDocxToPdf(version.docxFileId());
    reportRepository.updateCurrentVersionPdfFile(
        report.id(),
        report.currentVersionNo(),
        pdf.fileId(),
        principal.userId()
    );
    return pdf;
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

  private String deliveryType(AuthPrincipal principal) {
    if (principal.clientType() == ClientType.INTERNAL) {
      return "INTERNAL_DOWNLOAD";
    }
    return "OFFICIAL_DOWNLOAD";
  }
}
