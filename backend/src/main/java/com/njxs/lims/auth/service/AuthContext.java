package com.njxs.lims.auth.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import java.util.Optional;

public final class AuthContext {

  private static final ThreadLocal<AuthPrincipal> CURRENT = new ThreadLocal<>();

  private AuthContext() {
  }

  public static void set(AuthPrincipal principal) {
    CURRENT.set(principal);
  }

  public static AuthPrincipal require() {
    AuthPrincipal principal = CURRENT.get();
    if (principal == null) {
      throw new AuthenticationFailedException("UNAUTHORIZED", "未登录或登录已失效");
    }
    return principal;
  }

  public static Optional<AuthPrincipal> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static void clear() {
    CURRENT.remove();
  }
}
