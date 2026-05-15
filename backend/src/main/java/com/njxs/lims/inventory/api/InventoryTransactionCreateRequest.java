package com.njxs.lims.inventory.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record InventoryTransactionCreateRequest(
    @NotNull UUID inventoryBatchId,
    @NotBlank String txnType,
    @NotNull BigDecimal quantity,
    String reason
) {
}
