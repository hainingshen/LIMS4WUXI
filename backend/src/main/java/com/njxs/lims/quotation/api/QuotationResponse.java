package com.njxs.lims.quotation.api;

import com.njxs.lims.quotation.domain.Quotation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record QuotationResponse(
    UUID id,
    String bizNo,
    UUID projectId,
    String status,
    String currency,
    BigDecimal totalAmount,
    LocalDate validUntil
) {

  public static QuotationResponse from(Quotation quotation) {
    return new QuotationResponse(
        quotation.getId(),
        quotation.getBizNo(),
        quotation.getProjectId(),
        quotation.getStatus(),
        quotation.getCurrency(),
        quotation.getTotalAmount(),
        quotation.getValidUntil()
    );
  }
}
