package com.njxs.lims.masterdata.api;

import jakarta.validation.constraints.NotBlank;

public record SystemConfigUpsertRequest(
    @NotBlank String configKey,
    @NotBlank String configValue,
    @NotBlank String valueType,
    @NotBlank String category,
    @NotBlank String editableByRole,
    String descriptionZh
) {
}
