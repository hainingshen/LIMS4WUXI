package com.njxs.lims.inventory.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryBatchRecord(
    UUID id,
    String batchNo,
    String name,
    String casNo,
    String specification,
    String manufacturer,
    LocalDate receivedAt,
    LocalDate expiryDate,
    BigDecimal quantity,
    String unit,
    String storageCondition,
    String storageLocation,
    LocalDate openedAt,
    String status
) {
}
