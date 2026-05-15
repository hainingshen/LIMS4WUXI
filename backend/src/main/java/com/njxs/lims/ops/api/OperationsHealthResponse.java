package com.njxs.lims.ops.api;

import java.time.Instant;
import java.util.Map;

public record OperationsHealthResponse(
    String status,
    Instant checkedAt,
    String traceId,
    Map<String, String> checks
) {
}
