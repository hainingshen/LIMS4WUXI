package com.njxs.lims.customer.domain;

import java.util.List;
import java.util.UUID;

public record PortalUserRecord(
    UUID id,
    UUID customerOrgId,
    String username,
    String displayName,
    String email,
    List<String> roleCodes,
    String status
) {
  public PortalUserRecord {
    roleCodes = List.copyOf(roleCodes);
  }
}
