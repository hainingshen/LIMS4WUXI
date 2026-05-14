package com.njxs.lims.permission;

import com.njxs.lims.auth.domain.AuthPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CoreDataScopeEvaluator implements DataScopeEvaluator {

  @Override
  public boolean supports(String actionCode, String scope) {
    return "ALL".equals(scope);
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    return true;
  }
}
