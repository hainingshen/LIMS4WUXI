package com.njxs.lims.customer.api;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CustomerOrgUpsertRequest(
    UUID id,
    @NotBlank String nameZh,
    String nameEn,
    @NotBlank String addressZh,
    String addressEn,
    String taxNo,
    String invoiceTitle
) {
}
