package com.njxs.lims.workcenter.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.permission.DataScopeEvaluator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkCenterDataScopeEvaluator implements DataScopeEvaluator {

  @Override
  public boolean supports(String actionCode, String scope) {
    return "WORK_CENTER_READ".equals(actionCode)
        && ("PROJECT_PARTICIPANT".equals(scope) || "TASK".equals(scope));
  }

  @Override
  public boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args) {
    return true;
  }
}
