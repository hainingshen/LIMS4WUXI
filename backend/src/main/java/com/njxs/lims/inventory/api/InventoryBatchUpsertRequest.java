package com.njxs.lims.inventory.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryBatchUpsertRequest(
    UUID id,
    @NotBlank String batchNo,
    @NotBlank String name,
    String casNo,
    String specification,
    String manufacturer,
    LocalDate receivedAt,
    LocalDate expiryDate,
    @NotNull BigDecimal quantity,
    @NotBlank String unit,
    @NotBlank String storageCondition,
    String storageLocation
) {
}
