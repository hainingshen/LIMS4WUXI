package com.njxs.lims.change.api;

import com.njxs.lims.change.domain.ChangeRequestItemRecord;

public record ChangeRequestItemResponse(
    String fieldPath,
    String fieldLabel,
    String oldValue,
    String newValue,
    String lockPolicy
) {

  public static ChangeRequestItemResponse from(ChangeRequestItemRecord record) {
    return new ChangeRequestItemResponse(
        record.fieldPath(),
        record.fieldLabel(),
        record.oldValue(),
        record.newValue(),
        record.lockPolicy()
    );
  }
}
