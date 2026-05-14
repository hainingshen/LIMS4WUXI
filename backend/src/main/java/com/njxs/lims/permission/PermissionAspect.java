package com.njxs.lims.permission;

import com.njxs.lims.auth.service.AuthContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PermissionAspect {

  private final PermissionService permissionService;

  public PermissionAspect(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @Around("@annotation(required)")
  public Object check(ProceedingJoinPoint pjp, RequiresAction required) throws Throwable {
    permissionService.assertAllowed(AuthContext.require(), required.value(), pjp.getArgs());
    return pjp.proceed();
  }
}
