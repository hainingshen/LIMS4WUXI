package com.njxs.lims.workcenter.domain;

import com.njxs.lims.auth.domain.AuthPrincipal;
import java.util.Set;
import java.util.UUID;

public record WorkCenterScope(
    UUID userId,
    boolean allAccess
) {

  private static final Set<String> ALL_ACCESS_ROLES = Set.of(
      "QUALITY_MANAGER",
      "DEPT_MANAGER",
      "GENERAL_MANAGER"
  );

  public static WorkCenterScope from(AuthPrincipal principal) {
    return new WorkCenterScope(
        principal.userId(),
        principal.roles().stream().anyMatch(ALL_ACCESS_ROLES::contains)
    );
  }
}
