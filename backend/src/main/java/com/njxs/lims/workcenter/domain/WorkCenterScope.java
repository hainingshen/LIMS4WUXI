package com.njxs.lims.workcenter.domain;

import com.njxs.lims.auth.domain.AuthPrincipal;
import java.util.Set;
import java.util.UUID;

public record WorkCenterScope(
    UUID userId,
    Set<String> roles,
    boolean allAccess
) {

  private static final Set<String> ALL_ACCESS_ROLES = Set.of(
      "SYSTEM_ADMIN",
      "QUALITY_MANAGER",
      "DEPT_MANAGER",
      "GENERAL_MANAGER"
  );

  public static WorkCenterScope from(AuthPrincipal principal) {
    return new WorkCenterScope(
        principal.userId(),
        Set.copyOf(principal.roles()),
        principal.roles().stream().anyMatch(ALL_ACCESS_ROLES::contains)
    );
  }

  public boolean hasRole(String roleCode) {
    return roles.contains(roleCode);
  }

  public boolean hasAnyRole(String... roleCodes) {
    for (String roleCode : roleCodes) {
      if (roles.contains(roleCode)) {
        return true;
      }
    }
    return false;
  }
}
