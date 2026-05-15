package com.njxs.lims.portal;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PortalAccessPolicy {

  public PortalListScope resolveListScope(AuthPrincipal principal) {
    if (principal.clientType() != ClientType.PORTAL || principal.orgId() == null) {
      return PortalListScope.NONE;
    }
    if (hasRole(principal, "CUSTOMER_ORG_ADMIN")) {
      return PortalListScope.ORG;
    }
    if (hasRole(principal, "CUSTOMER_APPLICANT")) {
      return PortalListScope.OWN;
    }
    return PortalListScope.NONE;
  }

  private boolean hasRole(AuthPrincipal principal, String roleCode) {
    return principal.roles().contains(roleCode);
  }
}
