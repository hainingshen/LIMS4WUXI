package com.njxs.lims.portal;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import java.util.UUID;
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

  public boolean canReadSubmission(
      AuthPrincipal principal,
      UUID customerOrgId,
      UUID applicantUserId
  ) {
    PortalListScope scope = resolveListScope(principal);
    if (scope == PortalListScope.ORG) {
      return principal.orgId().equals(customerOrgId);
    }
    if (scope == PortalListScope.OWN) {
      return principal.userId().equals(applicantUserId) && principal.orgId().equals(customerOrgId);
    }
    return false;
  }

  private boolean hasRole(AuthPrincipal principal, String roleCode) {
    return principal.roles().contains(roleCode);
  }
}
