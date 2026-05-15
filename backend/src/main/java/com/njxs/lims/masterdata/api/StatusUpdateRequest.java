package com.njxs.lims.masterdata.api;

import jakarta.validation.constraints.NotBlank;

public record StatusUpdateRequest(
    @NotBlank String status
) {
}
