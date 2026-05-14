package com.njxs.lims.sample.retention.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.sample.domain.Sample;
import com.njxs.lims.sample.persistence.SampleRepository;
import com.njxs.lims.sample.retention.api.SampleRetentionCreateRequest;
import com.njxs.lims.sample.retention.api.SampleRetentionDisposeRequest;
import com.njxs.lims.sample.retention.api.SampleRetentionExtendRequest;
import com.njxs.lims.sample.retention.api.SampleRetentionResponse;
import com.njxs.lims.sample.retention.domain.SampleRetentionRecord;
import com.njxs.lims.sample.retention.persistence.SampleRetentionRepository;
import java.time.Instant;
import java.time.LocalDate;
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
public class SampleRetentionService {

  private static final Set<String> DISPOSITION_PLANS = Set.of("RETURN", "DESTROY", "ARCHIVE", "EXTEND");
  private static final Set<String> DISPOSITIONS = Set.of("RETURN", "DESTROY", "ARCHIVE");
  private static final Set<String> DISPOSABLE_STATUSES = Set.of("RETAINED", "EXPIRING", "PENDING_DISPOSAL");

  private final SampleRetentionRepository retentionRepository;
  private final SampleRepository sampleRepository;

  public SampleRetentionService(
      SampleRetentionRepository retentionRepository,
      SampleRepository sampleRepository
  ) {
    this.retentionRepository = retentionRepository;
    this.sampleRepository = sampleRepository;
  }

  @Transactional
  public SampleRetentionResponse create(SampleRetentionCreateRequest request, AuthPrincipal principal) {
    String dispositionPlan = normalize(request.dispositionPlan());
    assertAllowed(dispositionPlan, DISPOSITION_PLANS, "dispositionPlan");
    Sample sample = sampleRepository.findById(request.sampleId())
        .orElseThrow(() -> notFound("sample", request.sampleId()));
    if (!sample.getProjectId().equals(request.projectId())) {
      throw new BusinessException(
          "SAMPLE_RETENTION_PROJECT_MISMATCH",
          "留样项目与样品项目不一致",
          HttpStatus.BAD_REQUEST,
          Map.of("sampleId", request.sampleId(), "projectId", request.projectId())
      );
    }

    Instant now = Instant.now();
    SampleRetentionRecord record = new SampleRetentionRecord(
        UUID.randomUUID(),
        retentionRepository.nextBizNo(now.atZone(ZoneOffset.UTC).getYear()),
        request.sampleId(),
        request.projectId(),
        request.reportId(),
        request.retainedQuantity(),
        request.quantityUnit(),
        request.storageCondition(),
        request.storageLocation(),
        now,
        principal.userId(),
        request.retainUntil(),
        dispositionPlan,
        "PENDING_REGISTER",
        null
    );
    return SampleRetentionResponse.from(retentionRepository.save(record));
  }

  public PageResponse<SampleRetentionResponse> list(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        retentionRepository.list(normalizedPageSize, offset).stream()
            .map(SampleRetentionResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        retentionRepository.count()
    );
  }

  @Transactional
  public SampleRetentionResponse register(UUID id, AuthPrincipal principal) {
    SampleRetentionRecord record = retention(id);
    requireStatus(record, Set.of("PENDING_REGISTER"));
    return SampleRetentionResponse.from(retentionRepository.updateStatus(id, "RETAINED", principal.userId()));
  }

  @Transactional
  public SampleRetentionResponse extend(
      UUID id,
      SampleRetentionExtendRequest request,
      AuthPrincipal principal
  ) {
    SampleRetentionRecord record = retention(id);
    requireStatus(record, Set.of("RETAINED", "EXPIRING"));
    if (!request.retainUntil().isAfter(record.retainUntil())) {
      throw new BusinessException(
          "SAMPLE_RETENTION_EXTENSION_INVALID",
          "延期日期必须晚于当前留样日期",
          HttpStatus.BAD_REQUEST,
          Map.of("currentRetainUntil", record.retainUntil(), "retainUntil", request.retainUntil())
      );
    }
    return SampleRetentionResponse.from(
        retentionRepository.extend(id, request.retainUntil(), request.reason(), principal.userId())
    );
  }

  @Transactional
  public SampleRetentionResponse dispose(
      UUID id,
      SampleRetentionDisposeRequest request,
      AuthPrincipal principal
  ) {
    SampleRetentionRecord record = retention(id);
    requireStatus(record, DISPOSABLE_STATUSES);
    String disposition = normalize(request.disposition());
    assertAllowed(disposition, DISPOSITIONS, "disposition");
    if (("RETURN".equals(disposition) || "DESTROY".equals(disposition))
        && !retentionRepository.paymentUnlocked(id)) {
      throw new BusinessException(
          "SAMPLE_RETENTION_PAYMENT_LOCKED",
          "付款门禁未通过，不能退还或销毁留样",
          HttpStatus.CONFLICT,
          Map.of("retentionId", id)
      );
    }
    return SampleRetentionResponse.from(retentionRepository.dispose(
        id,
        disposition,
        statusFor(disposition),
        request.reason(),
        request.proofFileId(),
        principal.userId()
    ));
  }

  private SampleRetentionRecord retention(UUID id) {
    return retentionRepository.findById(id)
        .orElseThrow(() -> notFound("sample_retention", id));
  }

  private void requireStatus(SampleRetentionRecord record, Set<String> expected) {
    if (!expected.contains(record.status())) {
      throw new BusinessException(
          "SAMPLE_RETENTION_STATUS_CONFLICT",
          "留样当前状态不可执行该操作",
          HttpStatus.CONFLICT,
          Map.of("expected", expected, "actual", record.status())
      );
    }
  }

  private String statusFor(String disposition) {
    return switch (disposition) {
      case "RETURN" -> "RETURNED";
      case "DESTROY" -> "DESTROYED";
      case "ARCHIVE" -> "ARCHIVED";
      default -> throw new BusinessException(
          "SAMPLE_RETENTION_DISPOSITION_INVALID",
          "留样处置方式不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("disposition", disposition)
      );
    };
  }

  private void assertAllowed(String value, Set<String> allowed, String field) {
    if (!allowed.contains(value)) {
      throw new BusinessException(
          "SAMPLE_RETENTION_VALUE_INVALID",
          "留样字段值无效",
          HttpStatus.BAD_REQUEST,
          Map.of("field", field, "value", value)
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

  private String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }
}
