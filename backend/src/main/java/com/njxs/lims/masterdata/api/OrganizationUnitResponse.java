package com.njxs.lims.masterdata.api;

import com.njxs.lims.masterdata.domain.OrganizationUnitRecord;
import java.util.UUID;

public record OrganizationUnitResponse(
    UUID id,
    UUID parentId,
    String name,
    String status
) {
  public static OrganizationUnitResponse from(OrganizationUnitRecord record) {
    return new OrganizationUnitResponse(record.id(), record.parentId(), record.name(), record.status());
  }
}
