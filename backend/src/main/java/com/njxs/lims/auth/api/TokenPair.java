package com.njxs.lims.auth.api;

import com.njxs.lims.auth.domain.ClientType;
import java.util.List;

public record TokenPair(
    String accessToken,
    String refreshToken,
    ClientType clientType,
    List<String> roles,
    long expiresInSeconds
) {
  public TokenPair {
    roles = List.copyOf(roles);
  }
}
