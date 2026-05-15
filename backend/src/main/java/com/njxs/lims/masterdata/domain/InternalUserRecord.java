package com.njxs.lims.masterdata.domain;

import java.util.List;
import java.util.UUID;

public record InternalUserRecord(
    UUID id,
    String username,
    String displayName,
    UUID organizationUnitId,
    String status,
    List<String> roleCodes
) {
  public InternalUserRecord {
    roleCodes = List.copyOf(roleCodes);
  }
}
