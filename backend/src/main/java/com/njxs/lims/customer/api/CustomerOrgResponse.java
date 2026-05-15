package com.njxs.lims.customer.api;

import com.njxs.lims.customer.domain.CustomerOrgRecord;
import java.util.UUID;

public record CustomerOrgResponse(
    UUID id,
    String nameZh,
    String nameEn,
    String addressZh,
    String addressEn,
    String taxNo,
    String invoiceTitle,
    String status
) {
  public static CustomerOrgResponse from(CustomerOrgRecord record) {
    return new CustomerOrgResponse(
        record.id(),
        record.nameZh(),
        record.nameEn(),
        record.addressZh(),
        record.addressEn(),
        record.taxNo(),
        record.invoiceTitle(),
        record.status()
    );
  }
}
