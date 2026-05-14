package com.njxs.lims.auth.service;

import com.njxs.lims.auth.api.CurrentUserResponse;
import com.njxs.lims.auth.api.LoginRequest;
import com.njxs.lims.auth.api.RefreshTokenRequest;
import com.njxs.lims.auth.api.TokenPair;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.AuthUser;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.jwt.JwtService;
import com.njxs.lims.auth.persistence.AuthUserRepository;
import com.njxs.lims.auth.store.RefreshTokenSession;
import com.njxs.lims.auth.store.RefreshTokenStore;
import com.njxs.lims.auth.store.TokenBlacklist;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthService {

  private final AuthUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenStore refreshTokenStore;
  private final TokenBlacklist tokenBlacklist;

  public AuthService(
      AuthUserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      RefreshTokenStore refreshTokenStore,
      TokenBlacklist tokenBlacklist
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenStore = refreshTokenStore;
    this.tokenBlacklist = tokenBlacklist;
  }

  public TokenPair login(LoginRequest request, ClientType clientType) {
    AuthUser user = userRepository.findActiveByUsernameAndClientType(request.username(), clientType)
        .orElseThrow(() -> invalidCredentials());
    if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
      throw invalidCredentials();
    }
    return issueTokenPair(user);
  }

  public TokenPair refresh(RefreshTokenRequest request) {
    if (tokenBlacklist.isRefreshTokenBlacklisted(request.refreshToken())) {
      throw invalidToken();
    }
    RefreshTokenSession session = refreshTokenStore.find(request.refreshToken())
        .orElseThrow(this::invalidToken);
    if (session.expiresAt().isBefore(Instant.now())) {
      refreshTokenStore.revoke(request.refreshToken());
      throw invalidToken();
    }
    tokenBlacklist.blacklistRefreshToken(request.refreshToken(), session.expiresAt());
    refreshTokenStore.revoke(request.refreshToken());
    AuthUser user = userRepository.findActiveByIdAndClientType(session.userId(), session.clientType())
        .orElseThrow(this::invalidToken);
    return issueTokenPair(user);
  }

  public void logout(AuthPrincipal principal, RefreshTokenRequest request) {
    tokenBlacklist.blacklistAccessTokenId(principal.tokenId(), jwtService.accessTokenExpiresAt());
    if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
      refreshTokenStore.find(request.refreshToken())
          .ifPresent(session -> tokenBlacklist.blacklistRefreshToken(request.refreshToken(), session.expiresAt()));
      refreshTokenStore.revoke(request.refreshToken());
    }
  }

  public CurrentUserResponse currentUser(AuthPrincipal principal) {
    AuthUser user = userRepository.findActiveByIdAndClientType(principal.userId(), principal.clientType())
        .orElseThrow(this::invalidToken);
    return new CurrentUserResponse(
        user.id(),
        user.username(),
        user.displayName(),
        user.clientType(),
        user.customerOrgId(),
        user.roleCodes(),
        userRepository.findDataScopes(user.roleCodes())
    );
  }

  private TokenPair issueTokenPair(AuthUser user) {
    String accessToken = jwtService.issueAccessToken(
        new AuthPrincipal(user.id(), user.clientType(), user.roleCodes(), user.customerOrgId(), null)
    );
    String refreshToken = refreshTokenStore.create(user.id(), user.clientType(), jwtService.refreshTokenExpiresAt());
    return new TokenPair(
        accessToken,
        refreshToken,
        user.clientType(),
        user.roleCodes(),
        jwtService.accessTokenTtlSeconds()
    );
  }

  private AuthenticationFailedException invalidCredentials() {
    return new AuthenticationFailedException("INVALID_CREDENTIALS", "用户名或密码错误");
  }

  private AuthenticationFailedException invalidToken() {
    return new AuthenticationFailedException("UNAUTHORIZED", "未登录或登录已失效");
  }
}
