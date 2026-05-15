package com.njxs.lims.masterdata.domain;

import java.util.UUID;

public record SystemConfigRecord(
    UUID id,
    String configKey,
    String configValue,
    String valueType,
    String category,
    String editableByRole,
    String descriptionZh
) {
}
