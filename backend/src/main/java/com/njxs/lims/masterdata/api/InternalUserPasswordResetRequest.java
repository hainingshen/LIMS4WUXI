package com.njxs.lims.masterdata.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InternalUserPasswordResetRequest(
    @NotBlank @Size(min = 8) String password
) {
}
