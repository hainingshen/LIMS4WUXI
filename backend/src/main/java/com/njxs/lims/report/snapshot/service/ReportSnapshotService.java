package com.njxs.lims.report.snapshot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.report.snapshot.domain.ReportFieldMapping;
import com.njxs.lims.report.snapshot.domain.ReportSnapshotSource;
import com.njxs.lims.report.snapshot.persistence.ReportFieldMappingRepository;
import com.njxs.lims.report.snapshot.persistence.ReportSnapshotSourceRepository;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReportSnapshotService {

  private final ReportFieldMappingRepository mappingRepository;
  private final ReportSnapshotSourceRepository sourceRepository;
  private final ObjectMapper objectMapper;

  public ReportSnapshotService(
      ReportFieldMappingRepository mappingRepository,
      ReportSnapshotSourceRepository sourceRepository,
      ObjectMapper objectMapper
  ) {
    this.mappingRepository = mappingRepository;
    this.sourceRepository = sourceRepository;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> buildSnapshot(UUID reportId, String templateCode) {
    String normalizedTemplateCode = requireText("templateCode", templateCode).toUpperCase(Locale.ROOT);
    List<ReportFieldMapping> mappings = mappingRepository.findByTemplateCode(normalizedTemplateCode);
    if (mappings.isEmpty()) {
      throw new BusinessException(
          "REPORT_FIELD_MAPPING_NOT_FOUND",
          "报告字段映射不存在",
          HttpStatus.CONFLICT,
          Map.of("templateCode", normalizedTemplateCode)
      );
    }
    ReportSnapshotSource source = sourceRepository.findByReportId(reportId)
        .orElseThrow(() -> notFound("report", reportId));

    Map<String, Object> snapshot = new LinkedHashMap<>();
    for (ReportFieldMapping mapping : mappings) {
      Object value = evaluate(source, mapping);
      if (mapping.required() && missing(value)) {
        throw new BusinessException(
            "REPORT_SNAPSHOT_FIELD_MISSING",
            "报告数据快照缺少必填字段",
            HttpStatus.CONFLICT,
            Map.of(
                "targetField", mapping.targetField(),
                "reportLabel", mapping.reportLabel(),
                "sourceEntity", mapping.sourceEntity(),
                "sourceFieldOrRule", mapping.sourceFieldOrRule()
            )
        );
      }
      snapshot.put(mapping.targetField(), value == null ? "" : value);
    }
    return snapshot;
  }

  private Object evaluate(ReportSnapshotSource source, ReportFieldMapping mapping) {
    return switch (mapping.sourceEntity()) {
      case "report" -> reportValue(source, mapping.sourceFieldOrRule());
      case "customer_org" -> customerOrgValue(source, mapping.sourceFieldOrRule());
      case "submission" -> submissionValue(source, mapping.sourceFieldOrRule());
      case "sample" -> sampleValue(source, mapping.sourceFieldOrRule());
      case "work_order" -> workOrderValue(source, mapping.sourceFieldOrRule());
      case "raw_record" -> rawRecordValue(source, mapping.sourceFieldOrRule());
      case "test_standard" -> source.rawRecordTestBasis();
      case "workflow" -> "/";
      default -> null;
    };
  }

  private Object reportValue(ReportSnapshotSource source, String rule) {
    return switch (rule) {
      case "bizNo" -> source.reportBizNo();
      case "remark" -> "";
      case "watermarkType" -> source.watermarkType();
      case "sealStatus" -> sealStatus(source);
      default -> null;
    };
  }

  private Object customerOrgValue(ReportSnapshotSource source, String rule) {
    return switch (rule) {
      case "nameZh" -> source.customerOrgNameZh();
      case "addressZh" -> source.customerOrgAddressZh();
      default -> null;
    };
  }

  private Object submissionValue(ReportSnapshotSource source, String rule) {
    return switch (rule) {
      case "manufacturerNameZh" -> defaultSlash(source.manufacturerNameZh());
      case "manufacturerAddressZh" -> defaultSlash(source.manufacturerAddressZh());
      default -> null;
    };
  }

  private Object sampleValue(ReportSnapshotSource source, String rule) {
    return switch (rule) {
      case "sampleNameZh" -> source.sampleNameZh();
      case "bizNo" -> source.sampleBizNo();
      case "lotBatch + '/' + productionDate" -> combineLotAndProductionDate(source);
      case "receivedAt or submittedSamplingDate" -> samplingDate(source);
      case "modelRef or sizeSpec" -> firstNonBlank(source.modelRef(), source.sizeSpec());
      case "quantity + quantityUnit" -> quantity(source.quantity(), source.quantityUnit());
      case "physicalState" -> source.physicalState();
      default -> null;
    };
  }

  private Object workOrderValue(ReportSnapshotSource source, String rule) {
    if ("min(startedAt) + '-' + max(completedAt)".equals(rule)) {
      return source.rawRecordTestTime();
    }
    return null;
  }

  private Object rawRecordValue(ReportSnapshotSource source, String rule) {
    return switch (rule) {
      case "testItem" -> source.rawRecordTestItem();
      case "equipmentUsages[].name + '(' + no + ')'" -> equipmentUsages(source);
      case "sterilityResults[].conclusion" -> sterilityConclusions(source);
      default -> null;
    };
  }

  private String sealStatus(ReportSnapshotSource source) {
    if ("RELEASED".equals(source.reportStatus()) && "UNLOCKED".equals(source.paymentGateStatus())) {
      return "SEALED";
    }
    return "NONE";
  }

  private String combineLotAndProductionDate(ReportSnapshotSource source) {
    return defaultBlank(source.lotBatch()) + "/";
  }

  private String samplingDate(ReportSnapshotSource source) {
    if (source.receivedDate() != null) {
      return source.receivedDate().toString();
    }
    if (source.receivedAt() != null) {
      return source.receivedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
    }
    return null;
  }

  private String quantity(BigDecimal quantity, String unit) {
    if (quantity == null || unit == null || unit.isBlank()) {
      return null;
    }
    return quantity.stripTrailingZeros().toPlainString() + unit.trim();
  }

  private String equipmentUsages(ReportSnapshotSource source) {
    JsonNode root = rawRecordData(source);
    JsonNode usages = root.path("equipmentUsages");
    if (!usages.isArray()) {
      return null;
    }
    String value = StreamSupport.stream(usages.spliterator(), false)
        .map(item -> equipmentUsage(item.path("name").asText(null), item.path("no").asText(null)))
        .filter(text -> text != null && !text.isBlank())
        .reduce((left, right) -> left + "；" + right)
        .orElse(null);
    return emptyToNull(value);
  }

  private String equipmentUsage(String name, String no) {
    if (name == null || name.isBlank()) {
      return null;
    }
    if (no == null || no.isBlank()) {
      return name.trim();
    }
    return name.trim() + "(" + no.trim() + ")";
  }

  private String sterilityConclusions(ReportSnapshotSource source) {
    JsonNode root = rawRecordData(source);
    JsonNode results = root.path("sterilityResults");
    if (!results.isArray()) {
      return null;
    }
    String value = StreamSupport.stream(results.spliterator(), false)
        .map(item -> item.path("conclusion").asText(null))
        .filter(text -> text != null && !text.isBlank())
        .map(String::trim)
        .distinct()
        .reduce((left, right) -> left + "；" + right)
        .orElse(null);
    return emptyToNull(value);
  }

  private JsonNode rawRecordData(ReportSnapshotSource source) {
    if (source.rawRecordData() == null || source.rawRecordData().isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(source.rawRecordData());
    } catch (JsonProcessingException exception) {
      throw new BusinessException(
          "RAW_RECORD_DATA_INVALID",
          "原始记录结构化数据无效",
          HttpStatus.CONFLICT,
          Map.of("rawRecordId", source.rawRecordId())
      );
    }
  }

  private boolean missing(Object value) {
    return value == null || (value instanceof String text && text.isBlank());
  }

  private String firstNonBlank(String first, String second) {
    String normalizedFirst = emptyToNull(first);
    return normalizedFirst == null ? emptyToNull(second) : normalizedFirst;
  }

  private String defaultSlash(String value) {
    return emptyToNull(value) == null ? "/" : value.trim();
  }

  private String defaultBlank(String value) {
    return value == null ? "" : value.trim();
  }

  private String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
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

  private BusinessException notFound(String entityType, UUID entityId) {
    return new BusinessException(
        "NOT_FOUND",
        "资源不存在",
        HttpStatus.NOT_FOUND,
        Map.of("entityType", entityType, "entityId", entityId)
    );
  }
}
