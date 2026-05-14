package com.njxs.lims.auth.api;

import com.njxs.lims.auth.domain.ClientType;
import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
    UUID id,
    String username,
    String displayName,
    ClientType clientType,
    UUID customerOrgId,
    List<String> roleCodes,
    List<String> dataScopes
) {
  public CurrentUserResponse {
    roleCodes = List.copyOf(roleCodes);
    dataScopes = List.copyOf(dataScopes);
  }
}
