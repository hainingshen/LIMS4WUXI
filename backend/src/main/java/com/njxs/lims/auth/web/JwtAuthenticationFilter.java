package com.njxs.lims.auth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.jwt.InvalidJwtException;
import com.njxs.lims.auth.jwt.JwtService;
import com.njxs.lims.auth.service.AuthContext;
import com.njxs.lims.auth.store.TokenBlacklist;
import com.njxs.lims.common.api.ApiError;
import com.njxs.lims.common.web.TraceIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final TokenBlacklist tokenBlacklist;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(
      JwtService jwtService,
      TokenBlacklist tokenBlacklist,
      ObjectMapper objectMapper
  ) {
    this.jwtService = jwtService;
    this.tokenBlacklist = tokenBlacklist;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      String token = bearerToken(request);
      if (token != null) {
        AuthPrincipal principal = jwtService.parseAccessToken(token);
        if (tokenBlacklist.isAccessTokenIdBlacklisted(principal.tokenId())) {
          writeUnauthorized(request, response);
          return;
        }
        AuthContext.set(principal);
      }
      filterChain.doFilter(request, response);
    } catch (InvalidJwtException exception) {
      writeUnauthorized(request, response);
    } finally {
      AuthContext.clear();
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.equals("/auth/internal/login")
        || path.equals("/auth/portal/login")
        || path.equals("/auth/refresh");
  }

  private String bearerToken(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    return authorization.substring("Bearer ".length()).trim();
  }

  private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        new ApiError("UNAUTHORIZED", "未登录或登录已失效", traceId(request), Map.of())
    );
  }

  private String traceId(HttpServletRequest request) {
    Object value = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    if (value instanceof String traceId && !traceId.isBlank()) {
      return traceId;
    }
    return "missing-trace-id";
  }
}
