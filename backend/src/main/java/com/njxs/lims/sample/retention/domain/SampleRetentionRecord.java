package com.njxs.lims.sample.retention.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SampleRetentionRecord(
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
}
