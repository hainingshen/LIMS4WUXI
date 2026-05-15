package com.njxs.lims.masterdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.audit.AuditLogRecord;
import com.njxs.lims.audit.AuditLogRepository;
import com.njxs.lims.audit.api.AuditLogResponse;
import com.njxs.lims.common.api.PageResponse;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.masterdata.api.BusinessRuleResponse;
import com.njxs.lims.masterdata.api.BusinessRuleUpsertRequest;
import com.njxs.lims.masterdata.api.SystemConfigResponse;
import com.njxs.lims.masterdata.api.SystemConfigUpsertRequest;
import com.njxs.lims.masterdata.domain.SystemConfigRecord;
import com.njxs.lims.masterdata.persistence.SystemConfigRepository;
import com.njxs.lims.rule.domain.BusinessRule;
import com.njxs.lims.rule.domain.RuleCacheInvalidatedEvent;
import com.njxs.lims.rule.persistence.BusinessRuleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SystemGovernanceService {

  private static final TypeReference<Map<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>() {
  };
  private static final Set<String> VALUE_TYPES = Set.of("INT", "DECIMAL", "STRING", "BOOLEAN", "JSON");

  private final SystemConfigRepository systemConfigRepository;
  private final BusinessRuleRepository businessRuleRepository;
  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  public SystemGovernanceService(
      SystemConfigRepository systemConfigRepository,
      BusinessRuleRepository businessRuleRepository,
      AuditLogRepository auditLogRepository,
      ObjectMapper objectMapper,
      ApplicationEventPublisher eventPublisher
  ) {
    this.systemConfigRepository = systemConfigRepository;
    this.businessRuleRepository = businessRuleRepository;
    this.auditLogRepository = auditLogRepository;
    this.objectMapper = objectMapper;
    this.eventPublisher = eventPublisher;
  }

  public List<SystemConfigResponse> listSystemConfigs() {
    return systemConfigRepository.list().stream()
        .map(SystemConfigResponse::from)
        .toList();
  }

  public SystemConfigResponse upsertSystemConfig(SystemConfigUpsertRequest request) {
    String valueType = normalizeCode("valueType", request.valueType());
    validateConfigValue(valueType, requireText("configValue", request.configValue()));
    return SystemConfigResponse.from(systemConfigRepository.upsert(new SystemConfigRecord(
        UUID.randomUUID(),
        normalizeCode("configKey", request.configKey()),
        request.configValue().trim(),
        valueType,
        normalizeCode("category", request.category()),
        normalizeCode("editableByRole", request.editableByRole()),
        normalizeOptionalText(request.descriptionZh())
    )));
  }

  public List<BusinessRuleResponse> listBusinessRules() {
    return businessRuleRepository.list().stream()
        .map(rule -> BusinessRuleResponse.from(rule, jsonObject(rule.actionPayloadJson())))
        .toList();
  }

  public BusinessRuleResponse upsertBusinessRule(BusinessRuleUpsertRequest request) {
    String ruleCode = normalizeCode("ruleCode", request.ruleCode());
    BusinessRule saved = businessRuleRepository.upsert(new BusinessRule(
        UUID.randomUUID(),
        ruleCode,
        requireText("nameZh", request.nameZh()),
        normalizeCode("triggerType", request.triggerType()),
        requireText("conditionExpr", request.conditionExpr()),
        normalizeCode("actionCode", request.actionCode()),
        jsonString(request.actionPayload()),
        request.priority(),
        Boolean.TRUE.equals(request.enabled()),
        normalizeOptionalText(request.note())
    ));
    eventPublisher.publishEvent(new RuleCacheInvalidatedEvent(ruleCode));
    return BusinessRuleResponse.from(saved, jsonObject(saved.actionPayloadJson()));
  }

  public PageResponse<AuditLogResponse> listAuditLogs(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        auditLogRepository.list(normalizedPageSize, offset).stream()
            .map(this::auditLogResponse)
            .toList(),
        normalizedPage,
        normalizedPageSize,
        auditLogRepository.count()
    );
  }

  private AuditLogResponse auditLogResponse(AuditLogRecord record) {
    return AuditLogResponse.from(
        record,
        jsonObject(record.beforeSnapshotJson()),
        jsonObject(record.afterSnapshotJson())
    );
  }

  private void validateConfigValue(String valueType, String value) {
    if (!VALUE_TYPES.contains(valueType)) {
      throw new BusinessException(
          "SYSTEM_CONFIG_VALUE_TYPE_INVALID",
          "系统配置值类型不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("valueType", valueType)
      );
    }
    try {
      switch (valueType) {
        case "INT" -> Integer.parseInt(value);
        case "DECIMAL" -> new BigDecimal(value);
        case "BOOLEAN" -> {
          if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("boolean expected");
          }
        }
        case "JSON" -> objectMapper.readTree(value);
        case "STRING" -> {
        }
        default -> throw new IllegalArgumentException("unknown value type");
      }
    } catch (RuntimeException | JsonProcessingException exception) {
      throw new BusinessException(
          "SYSTEM_CONFIG_VALUE_INVALID",
          "系统配置值与值类型不匹配",
          HttpStatus.BAD_REQUEST,
          Map.of("valueType", valueType, "configValue", value)
      );
    }
  }

  private String jsonString(Map<String, Object> value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(
          "BUSINESS_RULE_ACTION_PAYLOAD_INVALID",
          "业务规则动作参数不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("reason", exception.getMessage())
      );
    }
  }

  private Map<String, Object> jsonObject(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(value, JSON_OBJECT_TYPE);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(
          "JSON_OBJECT_INVALID",
          "JSON对象不合法",
          HttpStatus.INTERNAL_SERVER_ERROR,
          Map.of("reason", exception.getMessage())
      );
    }
  }

  private String normalizeCode(String field, String value) {
    return requireText(field, value).toUpperCase(Locale.ROOT);
  }

  private String requireText(String field, String value) {
    String normalized = normalizeOptionalText(value);
    if (normalized == null) {
      throw new BusinessException(
          "VALIDATION_FAILED",
          "请求参数校验失败",
          HttpStatus.BAD_REQUEST,
          Map.of(field, "required")
      );
    }
    return normalized;
  }

  private String normalizeOptionalText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
