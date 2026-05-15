package com.njxs.lims.masterdata.domain;

public record LegacyStatusMappingRecord(
    String legacyEntityType,
    String legacyStatusText,
    String targetEntityType,
    String targetStatus,
    String migrationMode,
    Integer qualityScoreDelta,
    String note
) {
}
