package com.njxs.lims.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReceiptCreateRequest(
    @NotNull UUID projectId,
    UUID reportId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull Instant receivedAt,
    @NotBlank String method,
    UUID voucherFileId,
    String note
) {
}
