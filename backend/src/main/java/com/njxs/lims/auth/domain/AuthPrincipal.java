package com.njxs.lims.auth.domain;

import java.util.List;
import java.util.UUID;

public record AuthPrincipal(
    UUID userId,
    ClientType clientType,
    List<String> roles,
    UUID orgId,
    String tokenId
) {
  public AuthPrincipal {
    roles = List.copyOf(roles);
  }
}
