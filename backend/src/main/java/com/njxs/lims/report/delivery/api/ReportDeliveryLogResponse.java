package com.njxs.lims.report.delivery.api;

import com.njxs.lims.report.delivery.domain.ReportDeliveryLogRecord;
import java.time.Instant;
import java.util.UUID;

public record ReportDeliveryLogResponse(
    UUID id,
    UUID reportId,
    UUID reportVersionId,
    String deliveryType,
    String deliveredTo,
    UUID deliveredBy,
    Instant deliveredAt,
    String clientType,
    String paymentGateStatus,
    UUID fileId,
    String traceId
) {

  public static ReportDeliveryLogResponse from(ReportDeliveryLogRecord record) {
    return new ReportDeliveryLogResponse(
        record.id(),
        record.reportId(),
        record.reportVersionId(),
        record.deliveryType(),
        record.deliveredTo(),
        record.deliveredBy(),
        record.deliveredAt(),
        record.clientType(),
        record.paymentGateStatus(),
        record.fileId(),
        record.traceId()
    );
  }
}
