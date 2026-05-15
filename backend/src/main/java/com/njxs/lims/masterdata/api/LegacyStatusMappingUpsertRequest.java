package com.njxs.lims.masterdata.api;

import jakarta.validation.constraints.NotBlank;

public record LegacyStatusMappingUpsertRequest(
    @NotBlank String legacyEntityType,
    @NotBlank String legacyStatusText,
    @NotBlank String targetEntityType,
    @NotBlank String targetStatus,
    @NotBlank String migrationMode,
    Integer qualityScoreDelta,
    String note
) {
}
