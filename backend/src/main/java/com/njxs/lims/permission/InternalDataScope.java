package com.njxs.lims.permission;

import com.njxs.lims.auth.domain.AuthPrincipal;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public record InternalDataScope(
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

  public static InternalDataScope from(AuthPrincipal principal) {
    Set<String> roleSet = Set.copyOf(principal.roles());
    return new InternalDataScope(
        principal.userId(),
        roleSet,
        roleSet.stream().anyMatch(ALL_ACCESS_ROLES::contains)
    );
  }

  public boolean hasRole(String roleCode) {
    return roles.contains(roleCode);
  }

  public boolean hasAnyRole(String... roleCodes) {
    return Arrays.stream(roleCodes).anyMatch(roles::contains);
  }
}
