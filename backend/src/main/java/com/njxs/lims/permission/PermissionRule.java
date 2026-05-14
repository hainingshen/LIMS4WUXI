package com.njxs.lims.permission;

public record PermissionRule(
    String actionCode,
    String roleCode,
    String access,
    String scope
) {

  public boolean isDeny() {
    return "DENY".equals(access);
  }

  public boolean grantsAccess() {
    return "ALLOW".equals(access) || "READ".equals(access) || "CONDITIONAL".equals(access);
  }
}
