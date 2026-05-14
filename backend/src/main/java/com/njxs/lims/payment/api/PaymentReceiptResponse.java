package com.njxs.lims.payment.api;

import com.njxs.lims.payment.domain.PaymentReceiptRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReceiptResponse(
    UUID id,
    String bizNo,
    UUID projectId,
    UUID reportId,
    BigDecimal amount,
    Instant receivedAt,
    UUID receivedBy,
    String method,
    UUID voucherFileId,
    String status
) {

  public static PaymentReceiptResponse from(PaymentReceiptRecord record) {
    return new PaymentReceiptResponse(
        record.id(),
        record.bizNo(),
        record.projectId(),
        record.reportId(),
        record.amount(),
        record.receivedAt(),
        record.receivedBy(),
        record.method(),
        record.voucherFileId(),
        record.status()
    );
  }
}
