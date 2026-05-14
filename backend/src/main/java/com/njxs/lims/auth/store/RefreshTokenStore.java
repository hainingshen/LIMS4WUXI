package com.njxs.lims.auth.store;

import com.njxs.lims.auth.domain.ClientType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {

  String create(UUID userId, ClientType clientType, Instant expiresAt);

  Optional<RefreshTokenSession> find(String refreshToken);

  void revoke(String refreshToken);
}
