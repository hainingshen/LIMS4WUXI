package com.njxs.lims.quotation.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record QuotationCreateRequest(
    @NotEmpty List<@Valid Item> items,
    @DecimalMin("0.00") BigDecimal discountAmount,
    String note
) {

  public record Item(
      @NotBlank String testName,
      @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
      @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal quantity
  ) {
  }
}
