package com.njxs.lims.masterdata.api;

import com.njxs.lims.masterdata.domain.InternalUserRecord;
import java.util.List;
import java.util.UUID;

public record InternalUserResponse(
    UUID id,
    String username,
    String displayName,
    String email,
    UUID organizationUnitId,
    String status,
    List<String> roleCodes
) {
  public static InternalUserResponse from(InternalUserRecord record) {
    return new InternalUserResponse(
        record.id(),
        record.username(),
        record.displayName(),
        record.email(),
        record.organizationUnitId(),
        record.status(),
        record.roleCodes()
    );
  }
}
