package com.njxs.lims.change.api;

import jakarta.validation.constraints.NotBlank;

public record ChangeRequestApprovalRequest(
    @NotBlank String stage,
    @NotBlank String outcome,
    String comment
) {
}
