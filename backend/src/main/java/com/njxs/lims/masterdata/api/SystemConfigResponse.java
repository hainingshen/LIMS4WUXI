package com.njxs.lims.masterdata.api;

import com.njxs.lims.masterdata.domain.SystemConfigRecord;
import java.util.UUID;

public record SystemConfigResponse(
    UUID id,
    String configKey,
    String configValue,
    String valueType,
    String category,
    String editableByRole,
    String descriptionZh
) {
  public static SystemConfigResponse from(SystemConfigRecord record) {
    return new SystemConfigResponse(
        record.id(),
        record.configKey(),
        record.configValue(),
        record.valueType(),
        record.category(),
        record.editableByRole(),
        record.descriptionZh()
    );
  }
}
