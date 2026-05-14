package com.njxs.lims.quotation.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record QuotationItem(
    UUID id,
    UUID quotationId,
    String testName,
    BigDecimal unitPrice,
    BigDecimal quantity,
    BigDecimal lineAmount,
    int sortOrder
) {
}
