package com.njxs.lims.sample.retention.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SampleRetentionCreateRequest(
    @NotNull UUID sampleId,
    @NotNull UUID projectId,
    UUID reportId,
    @NotNull @Positive BigDecimal retainedQuantity,
    @NotBlank String quantityUnit,
    @NotBlank String storageCondition,
    @NotBlank String storageLocation,
    @NotNull LocalDate retainUntil,
    @NotBlank String dispositionPlan
) {
}
