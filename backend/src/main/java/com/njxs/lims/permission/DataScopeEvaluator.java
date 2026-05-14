package com.njxs.lims.permission;

import com.njxs.lims.auth.domain.AuthPrincipal;

public interface DataScopeEvaluator {

  boolean supports(String actionCode, String scope);

  boolean isAllowed(AuthPrincipal principal, String actionCode, String scope, Object[] args);
}
