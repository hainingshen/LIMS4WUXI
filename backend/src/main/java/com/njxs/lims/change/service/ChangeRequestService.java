package com.njxs.lims.change.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.change.api.ChangeRequestCreateRequest;
import com.njxs.lims.change.api.ChangeRequestItemRequest;
import com.njxs.lims.change.api.ChangeRequestResponse;
import com.njxs.lims.change.domain.ChangeRequestItemRecord;
import com.njxs.lims.change.domain.ChangeRequestRecord;
import com.njxs.lims.change.persistence.ChangeRequestRepository;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.form.domain.FormSchemaRecord;
import com.njxs.lims.form.service.FormSchemaService;
import com.njxs.lims.sample.domain.Sample;
import com.njxs.lims.sample.persistence.SampleRepository;
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
public class ChangeRequestService {

  private static final Set<String> TARGET_TYPES = Set.of("SUBMISSION", "CONTRACT", "SAMPLE", "PROJECT", "REPORT");
  private static final Set<String> CHANGE_TYPES = Set.of(
      "FIELD_CORRECTION", "SCOPE_CHANGE", "COST_PERIOD_CHANGE", "REPORT_CORRECTION", "OTHER"
  );

  private final ChangeRequestRepository changeRequestRepository;
  private final SampleRepository sampleRepository;
  private final FormSchemaService formSchemaService;

  public ChangeRequestService(
      ChangeRequestRepository changeRequestRepository,
      SampleRepository sampleRepository,
      FormSchemaService formSchemaService
  ) {
    this.changeRequestRepository = changeRequestRepository;
    this.sampleRepository = sampleRepository;
    this.formSchemaService = formSchemaService;
  }

  @Transactional
  public ChangeRequestResponse create(ChangeRequestCreateRequest request, AuthPrincipal principal) {
    String targetType = normalize(request.targetType());
    String changeType = normalize(request.changeType());
    assertAllowedValue(targetType, TARGET_TYPES, "targetType");
    assertAllowedValue(changeType, CHANGE_TYPES, "changeType");

    TargetSnapshot target = target(targetType, request.targetId());
    List<ChangeRequestItemRecord> items = request.items().stream()
        .map(item -> normalizeItem(target, item))
        .toList();
    String status = status(targetType, request.targetId(), items);
    Instant now = Instant.now();
    ChangeRequestRecord record = new ChangeRequestRecord(
        UUID.randomUUID(),
        changeRequestRepository.nextBizNo(now.atZone(ZoneOffset.UTC).getYear()),
        targetType,
        request.targetId(),
        target.bizNo(),
        changeType,
        request.reason(),
        status,
        principal.userId(),
        now,
        null,
        request.attachmentFileId(),
        items
    );
    return ChangeRequestResponse.from(changeRequestRepository.save(record));
  }

  public PageResponse<ChangeRequestResponse> list(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        changeRequestRepository.list(normalizedPageSize, offset).stream()
            .map(ChangeRequestResponse::from)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        changeRequestRepository.count()
    );
  }

  private ChangeRequestItemRecord normalizeItem(TargetSnapshot target, ChangeRequestItemRequest request) {
    String fieldKey = fieldKey(request.fieldPath());
    FieldPolicy policy = fieldPolicy(fieldKey, request.fieldLabel(), request.lockPolicy());
    String oldValue = target.value(fieldKey);
    return new ChangeRequestItemRecord(
        request.fieldPath(),
        policy.fieldLabel(),
        oldValue == null ? request.oldValue() : oldValue,
        request.newValue(),
        policy.lockPolicy()
    );
  }

  private String status(String targetType, UUID targetId, List<ChangeRequestItemRecord> items) {
    boolean hardLock = items.stream().anyMatch(item -> "HARD_LOCK".equals(item.lockPolicy()));
    if ("SAMPLE".equals(targetType) && hardLock && changeRequestRepository.issuedReportExistsForSample(targetId)) {
      return "NEEDS_REPORT_REVISION";
    }
    return "DRAFT";
  }

  private TargetSnapshot target(String targetType, UUID targetId) {
    if ("SAMPLE".equals(targetType)) {
      Sample sample = sampleRepository.findById(targetId)
          .orElseThrow(() -> notFound("sample", targetId));
      return new TargetSnapshot(sample.getBizNo(), sample);
    }
    return new TargetSnapshot(null, null);
  }

  private FieldPolicy fieldPolicy(String fieldKey, String requestLabel, String requestLockPolicy) {
    FormSchemaRecord schema = formSchemaService.current("SUBMISSION_VER15", 1);
    JsonNode field = schema.schemaJson().path("properties").path(fieldKey);
    String fieldLabel = field.path("title").asText(requestLabel);
    String lockPolicy = field.path("x-lock-policy").asText(requestLockPolicy);
    return new FieldPolicy(fieldLabel, normalize(lockPolicy));
  }

  private String fieldKey(String fieldPath) {
    int index = fieldPath.lastIndexOf('.');
    return index >= 0 ? fieldPath.substring(index + 1) : fieldPath;
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private void assertAllowedValue(String value, Set<String> allowed, String field) {
    if (!allowed.contains(value)) {
      throw new BusinessException(
          "INVALID_CHANGE_REQUEST_VALUE",
          "变更申请字段值无效",
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

  private record FieldPolicy(String fieldLabel, String lockPolicy) {
  }

  private record TargetSnapshot(String bizNo, Sample sample) {

    String value(String fieldKey) {
      if (sample == null) {
        return null;
      }
      return switch (fieldKey) {
        case "sampleNameZh" -> sample.getSampleNameZh();
        case "sampleNameEn" -> sample.getSampleNameEn();
        case "sterilizationState" -> sample.getSterilizationState();
        case "casCode" -> sample.getCasCode();
        case "modelRef" -> sample.getModelRef();
        case "sizeSpec" -> sample.getSizeSpec();
        case "lotBatch" -> sample.getLotBatch();
        case "rawMaterial" -> sample.getRawMaterial();
        case "packagingMaterial" -> sample.getPackagingMaterial();
        case "physicalState" -> sample.getPhysicalState();
        case "color" -> sample.getColor();
        case "density" -> sample.getDensity();
        case "stability" -> sample.getStability();
        case "solubility" -> sample.getSolubility();
        case "storageCondition" -> sample.getStorageCondition();
        case "intendedUse" -> sample.getIntendedUse();
        case "additionalInfo" -> sample.getAdditionalInfo();
        default -> null;
      };
    }
  }
}
