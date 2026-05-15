package com.njxs.lims.inventory.api;

import com.njxs.lims.inventory.domain.InventoryTransactionRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryTransactionResponse(
    UUID id,
    UUID inventoryBatchId,
    String txnType,
    BigDecimal quantity,
    UUID operatedBy,
    Instant operatedAt,
    String reason
) {

  public static InventoryTransactionResponse from(InventoryTransactionRecord record) {
    return new InventoryTransactionResponse(
        record.id(),
        record.inventoryBatchId(),
        record.txnType(),
        record.quantity(),
        record.operatedBy(),
        record.operatedAt(),
        record.reason()
    );
  }
}
