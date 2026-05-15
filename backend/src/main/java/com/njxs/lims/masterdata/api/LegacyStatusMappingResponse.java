package com.njxs.lims.masterdata.api;

import com.njxs.lims.masterdata.domain.LegacyStatusMappingRecord;

public record LegacyStatusMappingResponse(
    String legacyEntityType,
    String legacyStatusText,
    String targetEntityType,
    String targetStatus,
    String migrationMode,
    Integer qualityScoreDelta,
    String note
) {
  public static LegacyStatusMappingResponse from(LegacyStatusMappingRecord record) {
    return new LegacyStatusMappingResponse(
        record.legacyEntityType(),
        record.legacyStatusText(),
        record.targetEntityType(),
        record.targetStatus(),
        record.migrationMode(),
        record.qualityScoreDelta(),
        record.note()
    );
  }
}
