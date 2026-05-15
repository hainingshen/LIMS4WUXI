package com.njxs.lims.customer.api;

import com.njxs.lims.customer.domain.PortalUserRecord;
import java.util.List;
import java.util.UUID;

public record PortalUserResponse(
    UUID id,
    UUID customerOrgId,
    String username,
    String displayName,
    String email,
    List<String> roleCodes,
    String status
) {
  public static PortalUserResponse from(PortalUserRecord record) {
    return new PortalUserResponse(
        record.id(),
        record.customerOrgId(),
        record.username(),
        record.displayName(),
        record.email(),
        record.roleCodes(),
        record.status()
    );
  }
}
