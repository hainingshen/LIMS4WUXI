package com.njxs.lims.masterdata.api;

import com.njxs.lims.masterdata.domain.RoleRecord;

public record RoleResponse(
    String roleCode,
    String roleName,
    String status
) {
  public static RoleResponse from(RoleRecord record) {
    return new RoleResponse(record.roleCode(), record.roleName(), record.status());
  }
}
