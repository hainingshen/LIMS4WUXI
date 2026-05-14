package com.njxs.lims.auth.store;

import java.time.Instant;

public interface TokenBlacklist {

  void blacklistAccessTokenId(String tokenId, Instant expiresAt);

  boolean isAccessTokenIdBlacklisted(String tokenId);

  void blacklistRefreshToken(String refreshToken, Instant expiresAt);

  boolean isRefreshTokenBlacklisted(String refreshToken);
}
