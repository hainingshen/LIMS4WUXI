package com.njxs.lims.report.template.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.report.template.api.ReportTemplateCreateRequest;
import com.njxs.lims.report.template.api.ReportTemplateResponse;
import com.njxs.lims.report.template.domain.ReportTemplateRecord;
import com.njxs.lims.report.template.persistence.ReportTemplateRepository;
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
public class ReportTemplateService {

  private static final Set<String> TEMPLATE_TYPES = Set.of(
      "CONTRACT_EXPORT",
      "SAMPLE_FLOW_EXPORT",
      "REPORT",
      "EN_REPORT",
      "SPECIAL_REPORT"
  );
  private static final Set<String> AUTOMATION_LEVELS = Set.of("A", "B", "C");

  private final ReportTemplateRepository reportTemplateRepository;

  public ReportTemplateService(ReportTemplateRepository reportTemplateRepository) {
    this.reportTemplateRepository = reportTemplateRepository;
  }

  public PageResponse<ReportTemplateResponse> list(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        reportTemplateRepository.list(normalizedPageSize, offset).stream()
            .map(ReportTemplateResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        reportTemplateRepository.count()
    );
  }

  @Transactional
  public ReportTemplateResponse create(ReportTemplateCreateRequest request, AuthPrincipal principal) {
    String templateCode = code(request.templateCode());
    String templateVersion = requireText("templateVersion", request.templateVersion());
    String templateType = enumValue("templateType", request.templateType(), TEMPLATE_TYPES);
    String automationLevel = enumValue("automationLevel", request.automationLevel(), AUTOMATION_LEVELS);
    if (reportTemplateRepository.existsByCodeAndVersion(templateCode, templateVersion)) {
      throw new BusinessException(
          "REPORT_TEMPLATE_VERSION_EXISTS",
          "报告模板版本已存在",
          HttpStatus.CONFLICT,
          Map.of("templateCode", templateCode, "templateVersion", templateVersion)
      );
    }
    if (!reportTemplateRepository.fileExists(request.fileId())) {
      throw notFound("file_metadata", request.fileId());
    }
    ReportTemplateRecord record = new ReportTemplateRecord(
        UUID.randomUUID(),
        templateCode,
        templateVersion,
        requireText("templateName", request.templateName()),
        templateType,
        automationLevel,
        request.fileId(),
        "DRAFT",
        null,
        null,
        emptyToNull(request.legacyTemplateName())
    );
    return ReportTemplateResponse.from(reportTemplateRepository.save(record, principal.userId()));
  }

  @Transactional
  public ReportTemplateResponse publish(UUID id, AuthPrincipal principal) {
    ReportTemplateRecord current = template(id);
    if ("PUBLISHED".equals(current.status())) {
      return ReportTemplateResponse.from(current);
    }
    requireStatus(current, Set.of("DRAFT"), "REPORT_TEMPLATE_STATUS_CONFLICT");
    reportTemplateRepository.deprecatePublishedByCode(current.templateCode(), current.id(), principal.userId());
    return ReportTemplateResponse.from(reportTemplateRepository.publish(id, Instant.now(), principal.userId()));
  }

  @Transactional
  public ReportTemplateResponse deprecate(UUID id, AuthPrincipal principal) {
    ReportTemplateRecord current = template(id);
    if ("DEPRECATED".equals(current.status())) {
      return ReportTemplateResponse.from(current);
    }
    requireStatus(current, Set.of("DRAFT", "PUBLISHED"), "REPORT_TEMPLATE_STATUS_CONFLICT");
    return ReportTemplateResponse.from(reportTemplateRepository.deprecate(id, principal.userId()));
  }

  private ReportTemplateRecord template(UUID id) {
    return reportTemplateRepository.findById(id)
        .orElseThrow(() -> notFound("report_template", id));
  }

  private void requireStatus(ReportTemplateRecord record, Set<String> expected, String code) {
    if (!expected.contains(record.status())) {
      throw new BusinessException(
          code,
          "报告模板当前状态不可执行该操作",
          HttpStatus.CONFLICT,
          Map.of("expected", expected, "actual", record.status())
      );
    }
  }

  private String code(String value) {
    return requireText("templateCode", value).toUpperCase(Locale.ROOT);
  }

  private String enumValue(String field, String value, Set<String> allowed) {
    String normalized = requireText(field, value).toUpperCase(Locale.ROOT);
    if (!allowed.contains(normalized)) {
      throw new BusinessException(
          "INVALID_REPORT_TEMPLATE_VALUE",
          "报告模板字段值无效",
          HttpStatus.BAD_REQUEST,
          Map.of("field", field, "value", value)
      );
    }
    return normalized;
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

  private String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
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
