package com.njxs.lims.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReceiptRecord(
    UUID id,
    String bizNo,
    UUID projectId,
    UUID reportId,
    BigDecimal amount,
    Instant receivedAt,
    UUID receivedBy,
    String method,
    UUID voucherFileId,
    String note,
    String status
) {
}
