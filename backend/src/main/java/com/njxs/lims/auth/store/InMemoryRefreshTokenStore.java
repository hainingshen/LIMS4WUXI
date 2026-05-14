package com.njxs.lims.auth.store;

import com.njxs.lims.auth.domain.ClientType;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

  private final SecureRandom secureRandom = new SecureRandom();
  private final Map<String, RefreshTokenSession> sessions = new ConcurrentHashMap<>();

  @Override
  public String create(UUID userId, ClientType clientType, Instant expiresAt) {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    sessions.put(token, new RefreshTokenSession(userId, clientType, expiresAt));
    return token;
  }

  @Override
  public Optional<RefreshTokenSession> find(String refreshToken) {
    RefreshTokenSession session = sessions.get(refreshToken);
    if (session != null && session.expiresAt().isBefore(Instant.now())) {
      sessions.remove(refreshToken);
      return Optional.empty();
    }
    return Optional.ofNullable(session);
  }

  @Override
  public void revoke(String refreshToken) {
    sessions.remove(refreshToken);
  }
}
