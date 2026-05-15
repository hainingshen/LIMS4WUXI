package com.njxs.lims.masterdata.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record InternalUserUpdateRequest(
    @NotBlank String displayName,
    String email,
    UUID organizationUnitId,
    @NotEmpty List<String> roleCodes
) {
}
