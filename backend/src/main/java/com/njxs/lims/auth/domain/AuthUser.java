package com.njxs.lims.auth.domain;

import java.util.List;
import java.util.UUID;

public record AuthUser(
    UUID id,
    String username,
    String displayName,
    String passwordHash,
    ClientType clientType,
    UUID customerOrgId,
    List<String> roleCodes
) {
  public AuthUser {
    roleCodes = List.copyOf(roleCodes);
  }
}
