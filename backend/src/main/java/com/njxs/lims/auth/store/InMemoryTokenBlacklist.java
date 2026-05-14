package com.njxs.lims.auth.store;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryTokenBlacklist implements TokenBlacklist {

  private final Map<String, Instant> accessTokenIds = new ConcurrentHashMap<>();
  private final Map<String, Instant> refreshTokens = new ConcurrentHashMap<>();

  @Override
  public void blacklistAccessTokenId(String tokenId, Instant expiresAt) {
    if (tokenId != null && !tokenId.isBlank()) {
      accessTokenIds.put(tokenId, expiresAt);
    }
  }

  @Override
  public boolean isAccessTokenIdBlacklisted(String tokenId) {
    return isBlacklisted(accessTokenIds, tokenId);
  }

  @Override
  public void blacklistRefreshToken(String refreshToken, Instant expiresAt) {
    if (refreshToken != null && !refreshToken.isBlank()) {
      refreshTokens.put(refreshToken, expiresAt);
    }
  }

  @Override
  public boolean isRefreshTokenBlacklisted(String refreshToken) {
    return isBlacklisted(refreshTokens, refreshToken);
  }

  private boolean isBlacklisted(Map<String, Instant> values, String key) {
    Instant expiresAt = values.get(key);
    if (expiresAt == null) {
      return false;
    }
    if (expiresAt.isBefore(Instant.now())) {
      values.remove(key);
      return false;
    }
    return true;
  }
}
