package com.njxs.lims.permission;

import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.auth.service.AuthContext;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientTypeAspect {

  @Around("@annotation(requiredClient)")
  public Object checkMethod(ProceedingJoinPoint pjp, RequiresClient requiredClient) throws Throwable {
    return check(pjp, requiredClient);
  }

  @Around("@within(requiredClient)")
  public Object checkClass(ProceedingJoinPoint pjp, RequiresClient requiredClient) throws Throwable {
    return check(pjp, requiredClient);
  }

  private Object check(ProceedingJoinPoint pjp, RequiresClient requiredClient) throws Throwable {
    ClientType actual = AuthContext.require().clientType();
    boolean allowed = Arrays.stream(requiredClient.value()).anyMatch(expected -> expected == actual);
    if (!allowed) {
      throw new PermissionDeniedException("CLIENT_TYPE_" + actual.name());
    }
    return pjp.proceed();
  }
}
