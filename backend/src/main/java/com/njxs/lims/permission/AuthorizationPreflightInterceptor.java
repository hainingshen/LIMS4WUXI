package com.njxs.lims.permission;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthorizationPreflightInterceptor implements HandlerInterceptor, WebMvcConfigurer {

  private final PermissionService permissionService;

  public AuthorizationPreflightInterceptor(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this);
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }
    RequiresClient requiresClient = requiresClient(handlerMethod);
    RequiresAction requiresAction = requiresAction(handlerMethod);
    if (requiresClient == null && requiresAction == null) {
      return true;
    }
    AuthPrincipal principal = AuthContext.require();
    if (requiresClient != null) {
      assertClientType(principal.clientType(), requiresClient);
    }
    if (requiresAction != null) {
      permissionService.assertGranted(principal, requiresAction.value());
    }
    return true;
  }

  private RequiresClient requiresClient(HandlerMethod handlerMethod) {
    RequiresClient methodAnnotation =
        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequiresClient.class);
    if (methodAnnotation != null) {
      return methodAnnotation;
    }
    return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequiresClient.class);
  }

  private RequiresAction requiresAction(HandlerMethod handlerMethod) {
    return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequiresAction.class);
  }

  private void assertClientType(ClientType actual, RequiresClient requiresClient) {
    boolean allowed = Arrays.stream(requiresClient.value()).anyMatch(expected -> expected == actual);
    if (!allowed) {
      throw new PermissionDeniedException("CLIENT_TYPE_" + actual.name());
    }
  }
}
