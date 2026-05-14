package com.njxs.lims.change.domain;

public record ChangeRequestItemRecord(
    String fieldPath,
    String fieldLabel,
    String oldValue,
    String newValue,
    String lockPolicy
) {
}
