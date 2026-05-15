package com.njxs.lims.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryTransactionRecord(
    UUID id,
    UUID inventoryBatchId,
    String txnType,
    BigDecimal quantity,
    UUID operatedBy,
    Instant operatedAt,
    String reason
) {
}
