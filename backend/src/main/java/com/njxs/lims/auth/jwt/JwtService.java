package com.njxs.lims.auth.jwt;

import com.njxs.lims.auth.config.AuthProperties;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final AuthProperties properties;
  private final SecretKey key;

  public JwtService(AuthProperties properties) {
    this.properties = properties;
    this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String issueAccessToken(AuthPrincipal principal) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(accessTokenTtl());
    String tokenId = UUID.randomUUID().toString();
    var builder = Jwts.builder()
        .subject(principal.userId().toString())
        .id(tokenId)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .claim("clientType", principal.clientType().name())
        .claim("roles", principal.roles());
    if (principal.orgId() != null) {
      builder.claim("orgId", principal.orgId().toString());
    }
    return builder.signWith(key).compact();
  }

  public AuthPrincipal parseAccessToken(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(key)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      UUID userId = UUID.fromString(claims.getSubject());
      ClientType clientType = ClientType.valueOf(claims.get("clientType", String.class));
      UUID orgId = claims.get("orgId", String.class) == null
          ? null
          : UUID.fromString(claims.get("orgId", String.class));
      return new AuthPrincipal(userId, clientType, roles(claims), orgId, claims.getId());
    } catch (IllegalArgumentException | JwtException exception) {
      throw new InvalidJwtException();
    }
  }

  public long accessTokenTtlSeconds() {
    return accessTokenTtl().toSeconds();
  }

  public Instant accessTokenExpiresAt() {
    return Instant.now().plus(accessTokenTtl());
  }

  public Instant refreshTokenExpiresAt() {
    return Instant.now().plus(Duration.ofDays(properties.refreshTokenTtlDays()));
  }

  private Duration accessTokenTtl() {
    return Duration.ofMinutes(properties.accessTokenTtlMinutes());
  }

  private List<String> roles(Claims claims) {
    Object value = claims.get("roles");
    if (value instanceof List<?> items) {
      return items.stream().map(String::valueOf).toList();
    }
    return List.of();
  }
}
