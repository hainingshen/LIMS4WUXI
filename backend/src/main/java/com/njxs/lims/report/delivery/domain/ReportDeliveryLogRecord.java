package com.njxs.lims.report.delivery.domain;

import java.time.Instant;
import java.util.UUID;

public record ReportDeliveryLogRecord(
    UUID id,
    UUID reportId,
    UUID reportVersionId,
    String deliveryType,
    String deliveredTo,
    UUID deliveredBy,
    Instant deliveredAt,
    String clientType,
    String paymentGateStatus,
    UUID fileId
) {
}
