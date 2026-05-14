package com.njxs.lims.sample.retention.api;

import com.njxs.lims.sample.retention.domain.SampleRetentionRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SampleRetentionResponse(
    UUID id,
    String bizNo,
    UUID sampleId,
    UUID projectId,
    UUID reportId,
    BigDecimal retainedQuantity,
    String quantityUnit,
    String storageCondition,
    String storageLocation,
    Instant retainedAt,
    UUID retainedBy,
    LocalDate retainUntil,
    String dispositionPlan,
    String status,
    UUID proofFileId
) {

  public static SampleRetentionResponse from(SampleRetentionRecord record) {
    return new SampleRetentionResponse(
        record.id(),
        record.bizNo(),
        record.sampleId(),
        record.projectId(),
        record.reportId(),
        record.retainedQuantity(),
        record.quantityUnit(),
        record.storageCondition(),
        record.storageLocation(),
        record.retainedAt(),
        record.retainedBy(),
        record.retainUntil(),
        record.dispositionPlan(),
        record.status(),
        record.proofFileId()
    );
  }
}
