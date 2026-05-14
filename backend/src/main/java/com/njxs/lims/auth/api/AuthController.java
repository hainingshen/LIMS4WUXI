package com.njxs.lims.auth.api;

import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/internal/login")
  public TokenPair loginInternal(@Valid @RequestBody LoginRequest request) {
    return authService.login(request, ClientType.INTERNAL);
  }

  @PostMapping("/portal/login")
  public TokenPair loginPortal(@Valid @RequestBody LoginRequest request) {
    return authService.login(request, ClientType.PORTAL);
  }

  @PostMapping("/refresh")
  public TokenPair refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authService.refresh(request);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
    authService.logout(AuthContext.require(), request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public CurrentUserResponse me() {
    return authService.currentUser(AuthContext.require());
  }
}
