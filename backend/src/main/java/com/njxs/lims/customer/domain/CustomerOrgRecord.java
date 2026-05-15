package com.njxs.lims.customer.domain;

import java.util.UUID;

public record CustomerOrgRecord(
    UUID id,
    String nameZh,
    String nameEn,
    String addressZh,
    String addressEn,
    String taxNo,
    String invoiceTitle,
    String status
) {
}
