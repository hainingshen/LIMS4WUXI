package com.njxs.lims.change.api;

import jakarta.validation.constraints.NotBlank;

public record ChangeRequestItemRequest(
    @NotBlank String fieldPath,
    @NotBlank String fieldLabel,
    String oldValue,
    String newValue,
    @NotBlank String lockPolicy
) {
}
