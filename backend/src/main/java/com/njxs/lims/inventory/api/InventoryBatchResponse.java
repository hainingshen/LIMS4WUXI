package com.njxs.lims.inventory.api;

import com.njxs.lims.inventory.domain.InventoryBatchRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryBatchResponse(
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

  public static InventoryBatchResponse from(InventoryBatchRecord record) {
    return new InventoryBatchResponse(
        record.id(),
        record.batchNo(),
        record.name(),
        record.casNo(),
        record.specification(),
        record.manufacturer(),
        record.receivedAt(),
        record.expiryDate(),
        record.quantity(),
        record.unit(),
        record.storageCondition(),
        record.storageLocation(),
        record.openedAt(),
        record.status()
    );
  }
}
