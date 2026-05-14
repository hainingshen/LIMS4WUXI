package com.njxs.lims.sample.api;

import java.time.Instant;

public record SampleStockInRequest(
    String inventoryLocation,
    Instant stockedAt
) {
}
