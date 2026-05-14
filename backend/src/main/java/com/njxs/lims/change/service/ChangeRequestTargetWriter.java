package com.njxs.lims.change.service;

import com.njxs.lims.change.domain.ChangeRequestItemRecord;
import com.njxs.lims.change.domain.ChangeRequestRecord;
import com.njxs.lims.common.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChangeRequestTargetWriter {

  private final JdbcTemplate jdbcTemplate;

  public ChangeRequestTargetWriter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void apply(ChangeRequestRecord request, UUID actorUserId) {
    if ("SAMPLE".equals(request.targetType())) {
      applySample(request, actorUserId);
      return;
    }
    throw new BusinessException(
        "CHANGE_REQUEST_TARGET_UNSUPPORTED",
        "变更目标暂不支持生效",
        HttpStatus.CONFLICT,
        Map.of("targetType", request.targetType())
    );
  }

  private void applySample(ChangeRequestRecord request, UUID actorUserId) {
    for (ChangeRequestItemRecord item : request.items()) {
      String column = sampleColumn(fieldKey(item.fieldPath()));
      int updated = jdbcTemplate.update(
          "update sample set " + column + " = ?, updated_at = now(), updated_by = ?, version = version + 1 "
              + "where id = ? and deleted_at is null",
          item.newValue(),
          actorUserId,
          request.targetId()
      );
      if (updated != 1) {
        throw new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "sample", "entityId", request.targetId())
        );
      }
    }
  }

  private String sampleColumn(String fieldKey) {
    return switch (fieldKey) {
      case "sampleNameZh" -> "sample_name_zh";
      case "sampleNameEn" -> "sample_name_en";
      case "sterilizationState" -> "sterilization_state";
      case "casCode" -> "cas_code";
      case "modelRef" -> "model_ref";
      case "sizeSpec" -> "size_spec";
      case "lotBatch" -> "lot_batch";
      case "rawMaterial" -> "raw_material";
      case "packagingMaterial" -> "packaging_material";
      case "physicalState" -> "physical_state";
      case "color" -> "color";
      case "density" -> "density";
      case "stability" -> "stability";
      case "solubility" -> "solubility";
      case "storageCondition" -> "storage_condition";
      case "intendedUse" -> "intended_use";
      case "additionalInfo" -> "additional_info";
      default -> throw new BusinessException(
          "CHANGE_REQUEST_FIELD_UNSUPPORTED",
          "变更字段暂不支持生效",
          HttpStatus.BAD_REQUEST,
          Map.of("field", fieldKey)
      );
    };
  }

  private String fieldKey(String fieldPath) {
    int index = fieldPath.lastIndexOf('.');
    return index >= 0 ? fieldPath.substring(index + 1) : fieldPath;
  }
}
